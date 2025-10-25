package cz.voidium.vote;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import cz.voidium.config.VoteConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

public class VoteListener implements AutoCloseable {
    private static final int MAX_PACKET_SIZE = 4096;
    private static final int MAX_RSA_PAYLOAD_LENGTH = 392;
    private static final DateTimeFormatter LOG_TIME = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.systemDefault());
    private static final SecureRandom CHALLENGE_RANDOM = new SecureRandom();
    private static final short V2_MAGIC = (short) 0x733A;

    private final MinecraftServer server;
    private final VoteConfig config;
    private final PrivateKey privateKey;
    private final String sharedSecret;
    private final PendingVoteQueue pendingQueue;
    private final Logger logger;
    private final ExecutorService workerPool;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ServerSocket serverSocket;
    private ExecutorService acceptExecutor;

    public VoteListener(MinecraftServer server, VoteConfig config, PrivateKey privateKey, String sharedSecret, 
                        PendingVoteQueue pendingQueue, Logger logger) {
        this.server = server;
        this.config = config;
        this.privateKey = privateKey;
        this.sharedSecret = sharedSecret == null ? "" : sharedSecret;
        this.pendingQueue = pendingQueue;
        this.logger = logger;
        this.workerPool = Executors.newFixedThreadPool(4, namedThread("Voidium-VoteWorker"));
    }

    public void start() throws IOException {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        serverSocket = new ServerSocket();
        serverSocket.bind(new InetSocketAddress(config.getHost(), config.getPort()));
        logger.info("Vote listener bound to {}:{}", config.getHost(), config.getPort());

        acceptExecutor = Executors.newSingleThreadExecutor(namedThread("Voidium-VoteAccept"));
        CompletableFuture.runAsync(() -> {
            while (running.get()) {
                try {
                    Socket socket = serverSocket.accept();
                    workerPool.submit(() -> handleConnection(socket));
                } catch (IOException e) {
                    if (running.get()) {
                        logger.error("Vote listener accept failed", e);
                        notifyOps("§cVote listener failure: " + e.getMessage());
                    }
                }
            }
        }, acceptExecutor);
    }

    private void handleConnection(Socket socket) {
        String message = null;
        V2ParseResult v2Result = V2ParseResult.notAttempted();
        String challenge = sharedSecret.isBlank() ? "" : generateChallenge();
        OutputStream out = null;
    try (socket) {
            InputStream in = socket.getInputStream();
            out = socket.getOutputStream();
            if (logger.isDebugEnabled()) {
                logger.debug("Příchozí NuVotifier spojení z {}", socket.getRemoteSocketAddress());
            }
            if (!sharedSecret.isBlank()) {
                out.write(("VOTIFIER 2 " + challenge + "\n").getBytes(StandardCharsets.UTF_8));
                out.flush();
            }
            byte[] payload = readPayload(in);
            logPayload(payload);
            if (payload.length == 0) {
                throw new IllegalArgumentException("Empty vote payload");
            }
            v2Result = tryParseV2Vote(payload, challenge);
            VoteEvent event;
            if (v2Result.event() != null) {
                event = v2Result.event();
                message = "<NuVotifier v2 binary payload>";
            } else {
                message = new String(payload, StandardCharsets.UTF_8).trim();
                event = parseLegacyOrFallback(message, payload);
            }
            processVote(event);
            logger.info("Vote received from {} for player {}", event.serviceName(), event.username());
            if (v2Result.event() != null) {
                writeV2Response(out, "ok", null, null);
            }
        } catch (Exception e) {
            logger.error("Failed to process vote", e);
            if (message != null && !message.isEmpty()) {
                logger.debug("Raw vote payload: {}", message);
            }
            if (v2Result.attempted() && out != null) {
                try {
                    writeV2Response(out, "error", "decode", e.getMessage());
                } catch (IOException ignored) {
                }
            }
            notifyOps("§cVote processing error: " + e.getMessage());
        }
    }

    private V2ParseResult tryParseV2Vote(byte[] rawPayload, String challenge) throws Exception {
        if (sharedSecret.isBlank() || rawPayload.length < 4) {
            return V2ParseResult.notAttempted();
        }

        ByteBuffer buffer = ByteBuffer.wrap(rawPayload).order(ByteOrder.BIG_ENDIAN);
        short magic = buffer.getShort();
        if (magic != V2_MAGIC) {
            return V2ParseResult.notAttempted();
        }

        if (buffer.remaining() < 2) {
            throw new IllegalArgumentException("NuVotifier payload missing length field");
        }

        int length = buffer.getShort() & 0xFFFF;
        if (length <= 0 || length > buffer.remaining()) {
            throw new IllegalArgumentException("NuVotifier payload length invalid: " + length);
        }

        byte[] jsonBytes = new byte[length];
        buffer.get(jsonBytes);
        String wrapperJson = new String(jsonBytes, StandardCharsets.UTF_8).trim();

        JsonObject wrapper = JsonParser.parseString(wrapperJson).getAsJsonObject();
        if (!wrapper.has("payload") || !wrapper.has("signature")) {
            throw new IllegalArgumentException("NuVotifier wrapper missing payload or signature");
        }

        String payloadText = wrapper.get("payload").getAsString();
        String signatureBase64 = wrapper.get("signature").getAsString();
        byte[] providedSignature;
        try {
            providedSignature = Base64.getDecoder().decode(signatureBase64);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("NuVotifier signature isn't valid Base64", ex);
        }

        JsonObject json = JsonParser.parseString(payloadText).getAsJsonObject();
        String serviceName = json.get("serviceName").getAsString();
        String username = json.get("username").getAsString();
        String address = json.has("address") ? json.get("address").getAsString() : "";
        long timestamp = json.has("timestamp") ? json.get("timestamp").getAsLong() : System.currentTimeMillis();
        String payloadChallenge = json.has("challenge") ? json.get("challenge").getAsString() : "";

        if (challenge == null || challenge.isBlank()) {
            throw new IllegalStateException("Server challenge missing for NuVotifier v2 validation");
        }
        if (!challenge.equals(payloadChallenge)) {
            throw new IllegalArgumentException("NuVotifier challenge mismatch");
        }

        verifyToken(payloadText, providedSignature, serviceName);

        VoteEvent event = new VoteEvent(serviceName, username, address, timestamp);
        return new V2ParseResult(true, event);
    }

    private byte[] readPayload(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[512];
        int read;
        while ((read = in.read(buffer)) != -1) {
            if (read == 0) {
                break;
            }
            out.write(buffer, 0, read);
            if (out.size() >= MAX_PACKET_SIZE) {
                break;
            }
        }
        return out.toByteArray();
    }

    private void logPayload(byte[] payload) {
        if (payload == null) {
            logger.warn("Vote payload: null");
            return;
        }
        String text = new String(payload, StandardCharsets.UTF_8).replace("\r", "\\r").replace("\n", "\\n");
        StringBuilder hex = new StringBuilder();
        for (byte b : payload) {
            hex.append(String.format("%02X ", b));
        }
        logger.info("Vote payload ({} bytes): {}", payload.length, text);
        if (payload.length > 0) {
            logger.info("Vote payload HEX: {}", hex.toString().trim());
        }
    }

    private VoteEvent parseLegacyOrFallback(String message, byte[] rawPayload) throws Exception {
        if (privateKey != null) {
            VoteEvent event = tryLegacyRsa(rawPayload);
            if (event != null) {
                return event;
            }
            event = tryLegacyRsa(message);
            if (event != null) {
                return event;
            }
        }

        return parseLegacyVote(message);
    }

    private VoteEvent tryLegacyRsa(byte[] payload) throws Exception {
        if (payload == null || payload.length == 0) {
            return null;
        }
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            String decrypted = new String(cipher.doFinal(payload), StandardCharsets.UTF_8).trim();
            return parseLegacyVote(decrypted);
        } catch (Exception ignored) {
            return null;
        }
    }

    private VoteEvent tryLegacyRsa(String message) throws Exception {
        if (message == null) {
            return null;
        }
        if (message.length() > MAX_RSA_PAYLOAD_LENGTH) {
            logger.warn("Legacy vote payload exceeds {} characters ({}). Ignoring for safety.", MAX_RSA_PAYLOAD_LENGTH, message.length());
            return null;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(message);
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            String decrypted = new String(cipher.doFinal(decoded), StandardCharsets.UTF_8).trim();
            return parseLegacyVote(decrypted);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private void verifyToken(String payloadText, byte[] signature, String serviceName) throws Exception {
        if (signature == null || signature.length == 0) {
            throw new IllegalArgumentException("Missing NuVotifier signature");
        }
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(sharedSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] expected = mac.doFinal(payloadText.getBytes(StandardCharsets.UTF_8));
        if (!MessageDigest.isEqual(expected, signature)) {
            throw new IllegalArgumentException("Invalid NuVotifier signature for service " + serviceName);
        }
    }

    private VoteEvent parseLegacyVote(String payload) {
        if (payload.startsWith("{") && payload.endsWith("}")) {
            JsonObject json = JsonParser.parseString(payload).getAsJsonObject();
            String serviceName = json.get("serviceName").getAsString();
            String username = json.get("username").getAsString();
            String address = json.has("address") ? json.get("address").getAsString() : "";
            long timestamp = json.has("timestamp") ? json.get("timestamp").getAsLong() : System.currentTimeMillis();
            return new VoteEvent(serviceName, username, address, timestamp);
        }

        String[] lines = payload.split("\\r?\\n");
        if (lines.length >= 5 && "VOTE".equalsIgnoreCase(lines[0])) {
            String serviceName = lines[1];
            String username = lines[2];
            String address = lines[3];
            long timestamp = Long.parseLong(lines[4].trim());
            return new VoteEvent(serviceName, username, address, timestamp);
        }
        throw new IllegalArgumentException("Unrecognised vote payload");
    }

    private void processVote(VoteEvent event) {
        logVote(event);

        // Vždy proveď /say nebo broadcast příkaz (oznámení), i když hráč není online
        executeAnnounceCommands(event);

        // Ostatní příkazy pouze pokud je hráč online
        boolean playerOnline = server.getPlayerList().getPlayerByName(event.username()) != null;
        if (playerOnline) {
            executeRewardCommands(event);
            logger.info("Vote reward delivered immediately to online player: {}", event.username());
        } else {
            pendingQueue.enqueue(event);
            logger.info("Player {} is offline, vote queued for later delivery", event.username());
        }

        archiveVote(event);
    }

    // Provede pouze /say nebo broadcast příkazy
    private void executeAnnounceCommands(VoteEvent event) {
        List<String> commands = config.getCommands();
        if (commands == null || commands.isEmpty()) {
            return;
        }
        for (String commandTemplate : commands) {
            String command = commandTemplate.replace("%PLAYER%", event.username());
            String cmdLower = command.toLowerCase().trim();
            if (cmdLower.startsWith("say ") || cmdLower.startsWith("broadcast ")) {
                server.execute(() -> {
                    try {
                        CommandSourceStack source = server.createCommandSourceStack();
                        server.getCommands().performPrefixedCommand(source, command);
                    } catch (Exception e) {
                        logger.error("Announce command execution failed for vote", e);
                        notifyOps("§cVote announce command failed: " + e.getMessage());
                    }
                });
            }
        }
    }

    // Provede všechny příkazy kromě /say a broadcast
    private void executeRewardCommands(VoteEvent event) {
        List<String> commands = config.getCommands();
        if (commands == null || commands.isEmpty()) {
            return;
        }
        for (String commandTemplate : commands) {
            String command = commandTemplate.replace("%PLAYER%", event.username());
            String cmdLower = command.toLowerCase().trim();
            if (cmdLower.startsWith("say ") || cmdLower.startsWith("broadcast ")) {
                continue;
            }
            server.execute(() -> {
                try {
                    CommandSourceStack source = server.createCommandSourceStack();
                    server.getCommands().performPrefixedCommand(source, command);
                } catch (Exception e) {
                    logger.error("Reward command execution failed for vote", e);
                    notifyOps("§cVote reward command failed: " + e.getMessage());
                }
            });
        }
    }

    private void logVote(VoteEvent event) {
        if (!config.getLogging().isVoteLog()) {
            return;
        }
        Path path = config.getResolvedVoteLogFile();
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            String line = String.format("%s\t%s\t%s\t%s%n",
                    LOG_TIME.format(Instant.ofEpochMilli(event.timestamp())),
                    event.serviceName(),
                    event.username(),
                    event.address());
        Files.writeString(path, line, StandardCharsets.UTF_8,
            java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (IOException e) {
            logger.warn("Failed to write vote log", e);
        }
    }

    private void archiveVote(VoteEvent event) {
        if (!config.getLogging().isArchiveJson()) {
            return;
        }
    Path path = config.getResolvedArchivePath();
        JsonObject json = new JsonObject();
        json.addProperty("timestamp", event.timestamp());
        json.addProperty("service", event.serviceName());
        json.addProperty("player", event.username());
        json.addProperty("address", event.address());
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND)) {
                writer.write(json.toString());
                writer.newLine();
            }
        } catch (IOException e) {
            logger.warn("Failed to append vote archive", e);
        }
    }

    // původní executeCommands odstraněna, logika je nyní rozdělena do executeAnnounceCommands a executeRewardCommands

    private void notifyOps(String message) {
        if (!config.getLogging().isNotifyOpsOnError()) {
            return;
        }
        server.execute(() -> server.getPlayerList().getPlayers().forEach(player -> {
            if (server.getPlayerList().isOp(player.getGameProfile())) {
                player.sendSystemMessage(Component.literal(message));
            }
        }));
    }

    @Override
    public void close() {
        running.set(false);
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ignore) {}
        }
        if (acceptExecutor != null) {
            acceptExecutor.shutdownNow();
        }
        workerPool.shutdownNow();
    }

    private ThreadFactory namedThread(String name) {
        return runnable -> {
            Thread thread = new Thread(runnable, name + "-" + System.nanoTime());
            thread.setDaemon(true);
            return thread;
        };
    }

    private static String generateChallenge() {
        byte[] bytes = new byte[24];
        CHALLENGE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void writeV2Response(OutputStream out, String status, String cause, String error) throws IOException {
        JsonObject json = new JsonObject();
        json.addProperty("status", status);
        if (cause != null) {
            json.addProperty("cause", cause);
        }
        if (error != null) {
            json.addProperty("error", error);
        }
        out.write(json.toString().getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    private record V2ParseResult(boolean attempted, VoteEvent event) {
        static V2ParseResult notAttempted() {
            return new V2ParseResult(false, null);
        }
    }
}
