---
layout: default
title: Troubleshooting (CZ)
---

# 🔧 Řešení problémů

<div class="hero">
   <p>Časté problémy a jejich řešení pro <strong>Voidium</strong>. Zkontrolujte příslušnou sekci níže, než otevřete tiket podpory.</p>

   <h2>Rychlá navigace</h2>
   <div class="card-grid">
      <a class="card" href="#obecne">
         <div class="card-title"><span class="card-icon">⚙️</span>Obecné</div>
         <div class="card-desc">Mod se nenačítá, chyby configu</div>
      </a>
      <a class="card" href="#discord">
         <div class="card-title"><span class="card-icon">🤖</span>Discord Bot</div>
         <div class="card-desc">Bot offline, propojení selhává</div>
      </a>
      <a class="card" href="#web-panel">
         <div class="card-title"><span class="card-icon">🌐</span>Web Panel</div>
         <div class="card-desc">Nejde se připojit, problémy s auth</div>
      </a>
      <a class="card" href="#skiny">
         <div class="card-title"><span class="card-icon">🎨</span>Skiny</div>
         <div class="card-desc">Skiny se nenačítají, Steve hlava</div>
      </a>
      <a class="card" href="#hlasovani">
         <div class="card-title"><span class="card-icon">🗳️</span>Hlasování</div>
         <div class="card-desc">Hlasy nepřicházejí, RSA chyby</div>
      </a>
      <a class="card" href="#vykon">
         <div class="card-title"><span class="card-icon">📊</span>Výkon</div>
         <div class="card-desc">Poklesy TPS, lag</div>
      </a>
   </div>
</div>

---

## ⚙️ Obecné {#obecne}

### Mod se nenačítá / crash při startu

| Příznak | Příčina | Řešení |
|---------|---------|--------|
| `ClassNotFoundException: cz.voidium...` | Špatná verze MC/NeoForge | Ověřte MC 1.21.1–1.21.10, NeoForge 21.1.208+ |
| `UnsupportedClassVersionError` | Stará verze Javy | Nainstalujte **Java 21** nebo novější |
| `JsonSyntaxException` v configu | Poškozený config JSON | Smažte poškozený config soubor a restartujte — výchozí hodnoty se vygenerují |
| Mod se načte ale vše je vypnuto | `enableMod: false` v general.json | Nastavte `enableMod: true` |

### Config se neukládá

- Config soubory jsou v `config/voidium/`. Ujistěte se, že proces serveru má **práva na zápis** do tohoto adresáře.
- Po manuálních úpravách použijte `/voidium reload` pro hot-reload configů.
- Web panel Config Studio vytváří **zálohu** před každou změnou — zkontrolujte `config/voidium/backups/` pokud potřebujete vrátit změny.

### Příkazy nefungují

- Všechny příkazy vyžadují **OP level 2** nebo vyšší.
- Zkontrolujte, že je konkrétní modul zapnutý v `general.json`.
- Použijte `/voidium help` pro zobrazení všech dostupných příkazů.

---

## 🤖 Discord Bot {#discord}

### Bot nejde online

| Příznak | Příčina | Řešení |
|---------|---------|--------|
| `LOGIN_FAILED` v logu | Neplatný bot token | Regenerujte token na [Discord Developer Portal](https://discord.com/developers/applications) a aktualizujte `discord.json` |
| Bot startuje ale žádné slash příkazy | Chybí guild ID | Nastavte `guildId` v `discord.json` na ID vašeho Discord serveru |
| `Missing Access` chyby | Nedostatečná oprávnění bota | Přidělte botovi **Administrator** nebo minimálně: Send Messages, Manage Channels, Manage Roles, Read Message History |
| Bot online ale ignoruje příkazy | Špatné `linkChannelId` | Ověřte ID kanálů v `discord.json` — zapněte Developer Mode v Discordu pro kopírování ID |

### Propojení účtu selhává

- Hráč musí být **online** v Minecraftu, když spouští `/link` v Discordu.
- 6místný kód vyprší za **10 minut** — získejte nový ve hře.
- Zkontrolujte `maxAccountsPerDiscord` — pokud je nastaveno na 1, Discord uživatel musí nejdřív `/unlink`.
- Bot musí mít přístup do kanálu `linkChannelId`.

### Chat bridge nefunguje

- Ujistěte se, že `enableChatBridge: true` v `discord.json`.
- Ověřte správné nastavení `chatChannelId`.
- Pro webhook mód (avatary se skiny): vytvořte webhook v chat kanálu a vložte URL do `chatWebhookUrl`.

### Stavové zprávy se nezobrazují

- Nastavte `enableStatusMessages: true` v `discord.json`.
- Pokud je `statusChannelId` prázdný, stavové zprávy používají `chatChannelId`.

---

## 🌐 Web Panel {#web-panel}

### Nelze se připojit k web panelu

| Příznak | Příčina | Řešení |
|---------|---------|--------|
| Connection refused | Web modul vypnutý | Nastavte `enableWeb: true` v `general.json` |
| Connection timeout | Port blokovaný firewallem | Otevřete port `8081` (nebo vlastní port) ve firewallu / hostingovém panelu |
| Stránka se načte ale je prázdná | Chyba JavaScriptu | Vymažte cache prohlížeče, zkuste režim inkognito |
| `ERR_CONNECTION_RESET` | Nesoulad `bindAddress` | Použijte `0.0.0.0` pro naslouchání na všech rozhraních |

### Problémy s autentizací

- **"Invalid token"**: Admin token mohl být přegenerován. Zkontrolujte aktuální `adminToken` v `web.json`.
- **Bootstrap token vypršel**: Bootstrap tokeny jsou **jednorázové, 10 minut**. Vygenerujte nový příkazem `/voidium web`.
- **Session vypršela**: Výchozí TTL session je 120 minut. Zvyšte `sessionTtlMinutes` pokud potřebujete.
- **Cookie se nenastavuje**: Ujistěte se, že přistupujete přes HTTP (ne HTTPS, pokud nemáte nakonfigurovaný reverse proxy). Cookie `voidium_session` vyžaduje správné párování domény.

### Konzole nezobrazuje výstup

- Konzolový feed používá Server-Sent Events (SSE). Některé reverse proxy bufferují SSE — nakonfigurujte proxy aby zakázal buffering pro `/api/feeds`.
- Zkontrolujte, že `consoleChannelId` je nastavený v `discord.json` (webová konzole sdílí stejný Log4j appender).

---

## 🎨 Skiny {#skiny}

### Skiny se nenačítají (Steve/Alex fallback)

| Příznak | Příčina | Řešení |
|---------|---------|--------|
| Všichni hráči mají Steve skin | SkinRestorer vypnutý | Nastavte `enableSkinRestorer: true` v `general.json` |
| Skiny se načtou se zpožděním | Cache miss při prvním přihlášení | Normální — skin se stahuje z Mojang API při prvním příchodu a ukládá do cache na `skinCacheHours` (výchozí 24) |
| Skiny fungují ale občas se resetují | Cache vypršela | Zvyšte `skinCacheHours` v `general.json` |
| `Connection timed out` v logu | Mojang API nedostupné | Zkontrolujte internetové připojení serveru. Skiny se načtou z cache, pokud je dostupná. |

### SkinRestorer funguje jen v offline módu

To je záměr. V online módu Minecraft řeší skiny nativně. SkinRestorer se aktivuje pouze když `server.properties` má `online-mode=false`.

---

## 🗳️ Hlasování {#hlasovani}

### Hlasy nepřicházejí

| Příznak | Příčina | Řešení |
|---------|---------|--------|
| Žádné vote eventy | Vote modul vypnutý | Nastavte `enableVote: true` v `general.json` |
| Hlasovací stránka říká "server offline" | Špatný port / nedostupný | Ujistěte se, že Votifier port (config `port`, výchozí `8192`) je otevřený a forwardován |
| `Invalid RSA key` chyby | Poškozené klíče | Smažte složku `RSA/` v configu a restartujte — nové klíče se vygenerují |
| `Invalid shared secret` | V2 protocol nesoulad | Ověřte, že `sharedSecret` v `vote.json` odpovídá tokenu na hlasovací stránce |
| Hlasy přicházejí ale žádná odměna | Prázdný seznam `commands` | Přidejte příkazy odměn do pole `commands` v `vote.json` |

### Nastavení poprvé

1. Zapněte vote modul v `general.json`
2. Spusťte server — RSA klíče se vygenerují automaticky
3. Na hlasovací stránce použijte **Votifier** protokol a zadejte IP serveru + vote port
4. Pro V2 stránky také vložte `sharedSecret` z `vote.json`
5. Otestujte testovacím tlačítkem hlasovací stránky

---

## 📊 Výkon {#vykon}

### Poklesy TPS

- **Entity Cleaner:** Pokud poklesy TPS korelují s velkým počtem entit, zapněte Entity Cleaner a upravte interval.
- **Chat Bridge:** Webhook mód má mírně větší režii než běžný mód. Pokud je TPS kritické, použijte běžný chat bridge.
- **Stats:** Stats reporter běží denně a spotřebovává minimální prostředky. Neměl by způsobovat problémy s TPS.
- **AI funkce:** AI volání jsou asynchronní a neblokují server tick. Nicméně, mnoho současných AI požadavků může ovlivnit síť.

### Vysoké využití paměti

- Zkontrolujte `skinCacheHours` — velmi dlouhé TTL s mnoha unikátními hráči může zvětšit skin cache.
- Entity Cleaner pomáhá kontrolovat počet entit, což přímo ovlivňuje paměť.
- Thread pool web panelu škáluje dynamicky — pod velkou zátěží vytváří více vláken.

---

## 📋 Získání pomoci {#pomoc}

Pokud váš problém není uveden výše:

1. Zkontrolujte **serverový log** (`logs/latest.log`) pro chybové zprávy od `[Voidium]`
2. Zkontrolujte **audit log web panelu** pro nedávné změny
3. Zkuste `/voidium reload` pro opětovné načtení všech configů
4. Smažte konkrétní config soubor a restartujte pro regeneraci výchozích hodnot
5. Otevřete tiket na našem Discord serveru

## Související stránky

- <a href="Config_CZ.html">Konfigurace</a> — Reference všech konfiguračních souborů
- <a href="Commands_CZ.html">Příkazy</a> — Kompletní seznam příkazů
- <a href="Install_CZ.html">Instalace</a> — Průvodce instalací
