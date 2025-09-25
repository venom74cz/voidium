package cz.voidium.vote;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public final class VoteKeyUtil {
    private VoteKeyUtil() {}

    public static PrivateKey loadOrCreatePrivateKey(Path privateKeyPath, Path publicKeyPath) throws Exception {
        if (!Files.exists(privateKeyPath)) {
            generateKeyPair(privateKeyPath, publicKeyPath);
        }
        byte[] content = readPem(privateKeyPath, "PRIVATE KEY");
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(content);
        return KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

    public static void generateKeyPair(Path privateKeyPath, Path publicKeyPath) throws Exception {
        Path parent = privateKeyPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();

        writePem(privateKeyPath, "PRIVATE KEY", pair.getPrivate().getEncoded());
        writePem(publicKeyPath, "PUBLIC KEY", pair.getPublic().getEncoded());
    }

    private static byte[] readPem(Path file, String type) throws IOException {
        String pem = Files.readString(file, StandardCharsets.UTF_8)
                .replace("-----BEGIN " + type + "-----", "")
                .replace("-----END " + type + "-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(pem);
    }

    private static void writePem(Path file, String type, byte[] data) throws IOException {
        StringBuilder builder = new StringBuilder();
        builder.append("-----BEGIN ").append(type).append("-----\n");
        String base64 = Base64.getEncoder().encodeToString(data);
        for (int i = 0; i < base64.length(); i += 64) {
            int end = Math.min(i + 64, base64.length());
            builder.append(base64, i, end).append('\n');
        }
        builder.append("-----END ").append(type).append("-----\n");
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(file, builder.toString(), StandardCharsets.UTF_8);
    }

    public static PublicKey loadPublicKey(Path publicKeyPath) throws Exception {
        byte[] content = readPem(publicKeyPath, "PUBLIC KEY");
        X509EncodedKeySpec spec = new X509EncodedKeySpec(content);
        return KeyFactory.getInstance("RSA").generatePublic(spec);
    }
}
