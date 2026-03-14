---
layout: default
title: Vote systém (CZ)
---

# 🗳️ Vote systém

<div class="hero">
   <p><strong>Vote systém</strong> přijímá hlasy z hlasovacích stránek přes NuVotifier protokol (V1 RSA &amp; V2 HMAC), provádí odměnové příkazy a řadí hlasy offline hráčů do fronty.</p>

   <div class="note">
      Pro zapnutí: nastavte <code>enableVote: true</code> v <code>general.json</code>. Konfigurace v <code>votes.json</code>.
   </div>

   <h2>Rychlá navigace</h2>
   <div class="card-grid">
      <a class="card" href="#jak-to-funguje">
         <div class="card-title"><span class="card-icon">⚙️</span>Jak to funguje</div>
         <div class="card-desc">V1, V2, odměny, pending fronta</div>
      </a>
      <a class="card" href="#konfigurace">
         <div class="card-title"><span class="card-icon">📝</span>Konfigurace</div>
         <div class="card-desc">votes.json pole</div>
      </a>
      <a class="card" href="#prikazy">
         <div class="card-title"><span class="card-icon">⌨️</span>Příkazy</div>
         <div class="card-desc">Správa pending votů</div>
      </a>
      <a class="card" href="#logovani">
         <div class="card-title"><span class="card-icon">📋</span>Logování</div>
         <div class="card-desc">Vote log, archiv, diagnostika</div>
      </a>
   </div>
</div>

## ⚙️ Jak to funguje {#jak-to-funguje}

### Podpora protokolů

Vote listener Voidia podporuje **oba** protokoly současně na jednom portu:

| Protokol | Autentizace | Popis |
|----------|-------------|-------|
| **V1 (Legacy RSA)** | RSA 2048-bit klíčový pár | Klasický Votifier formát. Klíče se generují automaticky. |
| **V2 (NuVotifier)** | HMAC-SHA256 shared secret | JSON-based token formát. Secret se generuje automaticky (16 znaků). |

### Průběh hlasování

1. Hlasovací stránka pošle vote paket na `host:port`
2. Voidium detekuje verzi protokolu a validuje payload
3. Pokud je hráč **online**: odměnové příkazy se provedou okamžitě
4. Pokud je hráč **offline**: hlas se uloží do pending fronty
5. Když se hráč příště přihlásí, pending hlasy se vyplatí tiše

### Oznámení

Když je `announceVotes` zapnutý, broadcast zpráva se pošle všem hráčům. Cooldown (`announcementCooldown`, výchozí 300s) zabraňuje spamu z rychlých hlasů.

## 📝 Konfigurace {#konfigurace}

Soubor: <code>config/voidium/votes.json</code>

### Základní nastavení

| Pole | Typ | Výchozí | Popis |
|------|-----|---------|-------|
| `enabled` | boolean | `true` | Hlavní přepínač |
| `host` | string | `"0.0.0.0"` | Rozhraní pro navázání |
| `port` | int | `8192` | Port listeneru |
| `rsaPrivateKeyPath` | string | `"votifier_rsa.pem"` | Cesta k PKCS#8 PEM privátnímu klíči |
| `rsaPublicKeyPath` | string | `"votifier_rsa_public.pem"` | Cesta kam se zapíše veřejný klíč |
| `sharedSecret` | string | _(auto-generovaný)_ | HMAC secret pro V2. Auto-generovaný pokud prázdný. |

### Odměny

| Pole | Typ | Výchozí | Popis |
|------|-----|---------|-------|
| `commands` | string[] | `["tellraw %PLAYER% ...", "give %PLAYER% diamond 1"]` | Příkazy provedené za hlas. Placeholder: `%PLAYER%` |
| `announceVotes` | boolean | `true` | Broadcastovat oznámení o hlasech |
| `announcementMessage` | string | `&b%PLAYER% &7voted for the server...` | Text broadcastu. Placeholder: `%PLAYER%` |
| `announcementCooldown` | int | `300` | Sekundy mezi oznámeními |
| `maxVoteAgeHours` | int | `24` | Ignorovat hlasy starší než toto (anti-spam) |

### Logování

| Pole | Typ | Výchozí | Popis |
|------|-----|---------|-------|
| `logging.voteLog` | boolean | `true` | Zapisovat plaintext vote log |
| `logging.voteLogFile` | string | `"votes.log"` | Název log souboru |
| `logging.archiveJson` | boolean | `true` | Appendovat NDJSON analytiku |
| `logging.archivePath` | string | `"votes-history.ndjson"` | Cesta NDJSON souboru |
| `logging.notifyOpsOnError` | boolean | `true` | Notifikovat OPy při chybách |
| `logging.pendingQueueFile` | string | `"pending-votes.json"` | Soubor fronty offline hlasů |
| `logging.pendingVoteMessage` | string | `&8[&bVoidium&8] &aPaid out &e%COUNT% &apending votes!` | Zpráva při vyplacení. Placeholder: `%COUNT%` |

## ⌨️ Příkazy {#prikazy}

| Příkaz | Oprávnění | Popis |
|--------|-----------|-------|
| `/voidium votes pending` | OP | Zobrazí celkový počet pending hlasů |
| `/voidium votes pending <player>` | OP | Zobrazí pending hlasy pro hráče |
| `/voidium votes clear` | OP | Vyčistí frontu pending hlasů |

## 📋 Logování {#logovani}

Voidium poskytuje duální logování:

- **votes.log** — plaintext záznam každého hlasu (hráč, timestamp, služba)
- **votes-history.ndjson** — line-delimited JSON pro analytiku a zpracování dat

Oba soubory se ukládají do <code>config/voidium/storage/</code>.

<div class="note">
   <strong>RSA klíče:</strong> Při prvním spuštění Voidium auto-generuje 2048-bit RSA klíčový pár. Zkopírujte veřejný klíč z <code>votifier_rsa_public.pem</code> do konfigurace hlasovací stránky.
</div>

## Nastavení s hlasovacími stránkami

1. Spusťte server jednou pro auto-generování klíčů a configu
2. Zkopírujte veřejný klíč z `votifier_rsa_public.pem`
3. Na hlasovací stránce nastavte:
   - **IP serveru**: IP vašeho serveru
   - **Port**: `8192` (nebo vámi nastavený port)
   - **Veřejný klíč**: vložte obsah `votifier_rsa_public.pem`
4. Pro V2 stránky také zadejte `sharedSecret` z `votes.json`

## Další

- <a href="Commands_CZ.html">Příkazy</a>
- <a href="Config_CZ.html">Konfigurace</a>
