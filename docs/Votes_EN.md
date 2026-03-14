---
layout: default
title: Vote System (EN)
---

# 🗳️ Vote System

<div class="hero">
   <p><strong>Vote System</strong> receives votes from voting sites via the NuVotifier protocol (V1 RSA &amp; V2 HMAC), executes reward commands, and queues votes for offline players.</p>

   <div class="note">
      To enable: set <code>enableVote: true</code> in <code>general.json</code>. Configure in <code>votes.json</code>.
   </div>

   <h2>Jump to</h2>
   <div class="card-grid">
      <a class="card" href="#how-it-works">
         <div class="card-title"><span class="card-icon">⚙️</span>How it works</div>
         <div class="card-desc">V1, V2, rewards, pending queue</div>
      </a>
      <a class="card" href="#config">
         <div class="card-title"><span class="card-icon">📝</span>Configuration</div>
         <div class="card-desc">votes.json fields</div>
      </a>
      <a class="card" href="#commands">
         <div class="card-title"><span class="card-icon">⌨️</span>Commands</div>
         <div class="card-desc">Pending votes management</div>
      </a>
      <a class="card" href="#logging">
         <div class="card-title"><span class="card-icon">📋</span>Logging</div>
         <div class="card-desc">Vote log, archive, diagnostics</div>
      </a>
   </div>
</div>

## ⚙️ How it works {#how-it-works}

### Protocol support

Voidium's vote listener supports **both** protocols simultaneously on a single port:

| Protocol | Authentication | Description |
|----------|---------------|-------------|
| **V1 (Legacy RSA)** | RSA 2048-bit key pair | Classic Votifier format. Keys are auto-generated on first run. |
| **V2 (NuVotifier)** | HMAC-SHA256 shared secret | JSON-based token format. Secret is auto-generated (16 chars). |

### Vote flow

1. Voting site sends a vote packet to `host:port`
2. Voidium detects protocol version and validates the payload
3. If the player is **online**: reward commands execute immediately
4. If the player is **offline**: vote is saved to the pending queue
5. When the player next logs in, pending votes are paid out silently

### Announcements

When `announceVotes` is enabled, a broadcast message is sent to all players. A cooldown (`announcementCooldown`, default 300s) prevents spam from rapid votes.

## 📝 Configuration {#config}

File: <code>config/voidium/votes.json</code>

### Core settings

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `enabled` | boolean | `true` | Master switch |
| `host` | string | `"0.0.0.0"` | Interface to bind |
| `port` | int | `8192` | Listener port |
| `rsaPrivateKeyPath` | string | `"votifier_rsa.pem"` | Path to PKCS#8 PEM private key (relative to config folder) |
| `rsaPublicKeyPath` | string | `"votifier_rsa_public.pem"` | Path where public key is written (for voting sites) |
| `sharedSecret` | string | _(auto-generated)_ | HMAC secret for V2. Auto-generated if blank. |

### Rewards

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `commands` | string[] | `["tellraw %PLAYER% ...", "give %PLAYER% diamond 1"]` | Server commands executed per vote. Placeholder: `%PLAYER%` |
| `announceVotes` | boolean | `true` | Broadcast vote announcements |
| `announcementMessage` | string | `&b%PLAYER% &7voted for the server and received a reward!` | Broadcast text. Placeholder: `%PLAYER%` |
| `announcementCooldown` | int | `300` | Seconds between announcements |
| `maxVoteAgeHours` | int | `24` | Ignore votes older than this (anti-spam) |

### Logging

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `logging.voteLog` | boolean | `true` | Write plaintext vote log |
| `logging.voteLogFile` | string | `"votes.log"` | Log file name |
| `logging.archiveJson` | boolean | `true` | Append NDJSON analytics |
| `logging.archivePath` | string | `"votes-history.ndjson"` | NDJSON file path |
| `logging.notifyOpsOnError` | boolean | `true` | Notify OPs on listener errors |
| `logging.pendingQueueFile` | string | `"pending-votes.json"` | Offline vote queue file |
| `logging.pendingVoteMessage` | string | `&8[&bVoidium&8] &aPaid out &e%COUNT% &apending votes!` | Message on login payout. Placeholder: `%COUNT%` |

## ⌨️ Commands {#commands}

| Command | Permission | Description |
|---------|-----------|-------------|
| `/voidium votes pending` | OP | Show total pending votes |
| `/voidium votes pending <player>` | OP | Show pending votes for a player |
| `/voidium votes clear` | OP | Clear the pending vote queue |

## 📋 Logging {#logging}

Voidium provides dual logging:

- **votes.log** — plaintext record of every vote (player, timestamp, service)
- **votes-history.ndjson** — line-delimited JSON for analytics and data processing

Both files are stored in <code>config/voidium/storage/</code>.

<div class="note">
   <strong>RSA keys:</strong> On first run, Voidium auto-generates a 2048-bit RSA key pair. Copy the public key from <code>votifier_rsa_public.pem</code> to your voting site configuration.
</div>

## Setup with voting sites

1. Start the server once to auto-generate keys and config
2. Copy the public key from `votifier_rsa_public.pem`
3. On the voting site, configure:
   - **Server IP**: your server IP
   - **Port**: `8192` (or your configured port)
   - **Public key**: paste the contents of `votifier_rsa_public.pem`
4. For V2 sites, also provide the `sharedSecret` from `votes.json`

## Related

- <a href="Commands_EN.html">Commands</a>
- <a href="Config_EN.html">Configuration</a>
