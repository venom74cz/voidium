# Votifier Integration Plan

## 1. Cíl a rozsah
- Přijímat hlasy z veřejných hlasovacích portálů (např. Minecraft-MP, TopG, Craftlist) přes protokol *Votifier* (TCP) a případně moderní webhook/REST rozhraní (NuVotifier JSON).
- Bezpečně validovat hlasy, logovat je a spouštět odměny definované v konfiguraci.
- Integrovat výsledky do stávajících modulů (broadcast, oznámení, prefixy, restart manager) a umožnit budoucí rozšíření (měsíční soutěže, synchronizace na Discord).

## 2. Finální požadavky a vstupy
| Oblast | Hodnota |
| --- | --- |
| **Portály** | Libovolné stránky podporující *Votifier v2* (NuVotifier); přijmeme jakýkoli JSON vote packet. |
| **Síťová konfigurace** | TCP listener na konfigurovatelném portu (výchozí 8192); naslouchá na `0.0.0.0`. |
| **Autentizace** | RSA public key – všechny služby používají serverový veřejný klíč k šifrování (standardní Votifier klíče). |
| **Odměny** | List příkazů definovaných v konfiguraci; každý hlas spustí všechny příkazy (žádné item/XP special-case). |
| **Opakování** | Žádný cooldown – každý validní hlas okamžitě vyvolá odměny. |
| **Integrace** | Není potřeba (žádný Discord/Telegram ani GUI propojení). |
| **Persistovaná data** | Logování do souboru (`votes.log`) + volitelný JSON archiv hlasů. |
| **Antispam** | Nevyužíváme dedikované antispam mechanismy (žádný rate-limit/whitelist). |
| **Monitoring** | Při chybě ohlásit OP hráčům (broadcast) + zapsat do logu. |

## 3. Architektura řešení
1. **Síťový listener**
  - TCP server (Netty/Java NIO) naslouchající na konfigurovatelném portu, přijímá NuVotifier/Votifier v2 pakety.
  - Každý příchozí paket se dešifruje pomocí privátního klíče serveru; očekáváme JSON payload s údaji o hlasu.
2. **Bezpečnostní vrstva**
  - RSA dešifrování – validace, že zpráva byla zašifrována naším veřejným klíčem.
  - Validace formátu (`username`, `serviceName`, `timestamp`).
  - Logování nevalidních pokusů + okamžitý broadcast pro OP.
3. **Pipeline zpracování**
  - Deserializace na `VoteEvent` a okamžité spuštění (bez fronty/cooldownu).
  - Zápis do `votes.log` a volitelně do JSON archivu.
4. **Odměnový engine**
  - Každý hlas provede všechny příkazy definované v konfiguraci (`%PLAYER%` placeholder).
  - Volitelně podporovat per-service sady příkazů, pokud se v budoucnu rozhodneme.
5. **Integrace s Voidium**
  - Základní logging přes `LOGGER`.
  - Alert OP hráčům při chybě (pomocí existujícího broadcast API).
6. **Chybová tolerance**
  - Pokud vykonání příkazů selže, zapsat do logu a uložit hlas do `pending-votes.json` pro pozdější re-run.
  - Chyba listeneru → broadcast pro OP, aby mohli zasáhnout.

## 4. Návrh konfigurace
Navrhovaný soubor: `config/voidium/votes.json`

```json
{
  "listener": {
    "enabled": true,
    "host": "0.0.0.0",
    "port": 8192,
    "protocol": "votifier-v2",
    "rsaPrivateKeyPath": "votifier_rsa.pem",
    "rsaPublicKeyPath": "votifier_rsa_public.pem",
    "sharedSecret": ""
  },
  "rewards": {
    "commands": [
      "give %PLAYER% minecraft:diamond 1",
      "eco give %PLAYER% 250"
    ]
  },
  "logging": {
    "voteLog": true,
    "voteLogFile": "config/voidium/votes.log",
    "archiveJson": true,
    "archivePath": "config/voidium/votes-history.json",
    "notifyOpsOnError": true
  }
}
```

### Vysvětlení klíčů
- `listener` – minimální nastavení pro Votifier v2 + cesty k privátnímu i veřejnému klíči (relativní cesty se řeší vůči složce `config/voidium`). Pokud vyplníte `sharedSecret`, přepneme se do **NuVotifier v2** módu, po TCP handshaku posíláme standardní dvojici řádků `VOTIFIER 2` + `This server is using NuVotifier 2` a očekáváme JSON se `token` podpisem. Ponecháte-li `sharedSecret` prázdné, zůstává legacy RSA režim kompatibilní s Votifier v1.
- `rewards.commands` – seznam příkazů vykonaných při každém hlasu (`%PLAYER%` placeholder nahradíme jménem).
- `logging.voteLog` – zapíná/ vypíná logování; `notifyOpsOnError` spustí broadcast pro OP, když nastane chyba.

> Možná integrace: přidat `GeneralConfig` flag `enableVotes`, aby šlo modul zapnout/vypnout.

## 5. Implementační roadmapa
1. **Fáze 0 – Konfigurace a klíče**
   - Generátor RSA klíče (Votifier v1) nebo import existujícího.
   - CLI/příkaz `/voidium votifier keygen`.
2. **Fáze 1 – Listener + validace**
  - Implementace TCP listeneru s RSA dekódováním V2.
  - Validace a parsování JSON payloadu.
  - Logování a základní oznámení (OP při chybě).
3. **Fáze 2 – Odměnový engine**
  - Implementace příkazových odměn; placeholdery + queue do server threadu.
4. **Fáze 3 – Persistence a GUI**
   - Historie hlasů (`votes-history.json`), statistiky.
   - Integrace do `/voidium gui` (sekce Vote stats, reset button).
5. **Fáze 4 – Integrace s externími systémy**
  - (volitelné) rozšíření o HTTP webhook listener, pokud někdy bude potřeba.
6. **Fáze 5 – Optimalizace a hardening**
   - Failover queue, nastavení vláken, telemetry.
   - Dokumentace pro adminy.

## 6. Testování a validace
- **Jednotkové testy**
  - Parsování a validace hlasů (správné/špatné vstupy).
  - Cooldown logika, odměnové scénáře.
- **Integrační testy**
  - Mock Votifier klient → server.
  - Simulace vysoké zátěže (více hlasů v rychlém sledu).
- **Manuální testy**
  - Přes oficiální Votifier testovací nástroj.
  - Kontrola logů, queue, odměn, broadcastů.
- **Monitoring**
  - Logy v `logs/votifier.log`, alerty pro neplatné podpisy, odkazy na FAQ.

## 7. Rozšíření (doporučení)
- **Top hlasovač týdne/měsíce** – automatická soutěž s vyhlášením.
- **Statistiky v GUI** – zobrazit počet hlasů, poslední hlasující.
- **API pro pluginy** – event bus `VoteReceivedEvent` pro další módy.
- **Leaderboards** – integrace s databází nebo scoreboardy.
- **Multi-server synchronizace** – redis/pub-sub pro hlasování ve vícepřístupovém clusteru.

---
*Dokument aktualizujeme po doplnění sekce Požadavky a vstupy.*
