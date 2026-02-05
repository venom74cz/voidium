# Release Notes v1.3.7

## 🆕 Nové funkce

### 🎫 Vylepšený Ticket Systém
- **Dvousměrná komunikace**: Zprávy z Discord ticket kanálů se nyní zobrazují hráčům v Minecraftu
- **Nový formát příkazu**: `/ticket <reason> <message>` - vytvoří ticket s počáteční zprávou
- **Automatické zprávy**: První zpráva od hráče se okamžitě pošle do ticketu
- **Lepší UX**: Support vidí problém hráče hned po otevření ticketu
- **Formátování**: Podpora emoji a markdown formátování v ticket zprávách

### 🔗 Vylepšený Link Kanál
- **Inteligentní odpovědi**: Bot nyní reaguje na KAŽDOU zprávu v link kanálu
- **Kontrola stavu**: Automaticky zkontroluje jestli je uživatel již propojen
- **Informativní zprávy**: 
  - "jsi již propojen! UUID: `...`" pro propojené uživatele
  - "nejsi propojen! Zadej platný kód ze hry." pro nepropojené
- **Auto-delete**: Všechny zprávy se automaticky mažou pro soukromí
- **Okamžitá zpětná vazba**: Uživatelé dostanou odpověď na každý pokus o link

### 📊 Vylepšené Statistiky
- **Debug logging**: Rozsáhlé logování pro snadné debugování statistik
- **Detailní informace**: Loguje časy, channel IDs, sample counts
- **Lepší error handling**: Jasné error zprávy když něco selže
- **Kontrola konfigurace**: Ověřuje správnost nastavení před odesláním reportu

## 🐛 Opravy chyb

### 💾 Discord Channel/Role IDs
- **Kritická oprava**: Discord IDs se nyní ukládají jako stringy, ne jako čísla
- **Problém**: JavaScript ztrácel přesnost u velkých čísel (> 2^53)
- **Řešení**: 
  - `LongSerializationPolicy.STRING` v GSON
  - Whitelist string polí v JavaScriptu
  - Správné zobrazení IDs: `1368592825842798633` místo `1368592825842798600`
- **Postižená pole**: `chatChannelId`, `consoleChannelId`, `linkChannelId`, `statusChannelId`, `linkedRoleId`, `guildId`, `ticketCategoryId`, `supportRoleId`, `reportChannelId`

### 🎫 Ticket Config Inicializace
- **Opraveno**: TicketConfig se nyní správně inicializuje při startu serveru
- **Důsledek**: Ticket sekce je nyní viditelná na web panelu
- **Přidáno**: `TicketConfig.init(voidiumDir)` do Voidium konstruktoru

### 📁 Organizace Config Souborů
- **Přesunuto**: `voidium_links.json` → `config/voidium/links.json`
- **Důvod**: Lepší organizace, všechny config soubory na jednom místě
- **Konzistence**: Nyní všechny data soubory v `config/voidium/` složce

### 🌐 Web Panel - Reset to Defaults
- **Přidáno**: TicketConfig do locale reset handleru
- **Nyní funguje**: Reset to Defaults správně přeloží všechny ticket zprávy
- **Kompletní**: Všechny config třídy s applyLocale metodou jsou zahrnuty

## 🔧 Technické vylepšení

### 📝 Lepší Error Zprávy
- České error zprávy v `/ticket` příkazu
- Jasné informace o chybách pro uživatele
- Lepší debug logging napříč systémem

### 🎨 Code Quality
- Lepší struktura DiscordManager.onMessageReceived()
- Refactoring ticket vytváření s podporou zpráv
- Čitelnější a udržitelnější kód

### 🌍 Lokalizace
- Kompletní české překlady pro všechny nové funkce
- Konzistentní použití placeholder formátů
- Podpora pro reset defaults v obou jazycích

## 📋 Kompletní seznam změn od v1.3.5

### v1.3.6
- Základní implementace ticket systému
- Live grafy na dashboardu
- Web panel lokalizace
- API endpoint pro historii statistik

### v1.3.7
- Dvousměrná komunikace v ticketech
- Inteligentní link kanál
- Oprava Discord ID ukládání
- Enhanced debug logging
- Config organizace
- Ticket config inicializace
- Kompletní lokalizace

## 🔍 Poznámky

### Upgrade z v1.3.5 nebo v1.3.6
1. **Důležité**: Zkontroluj Discord Channel IDs ve web panelu - mohou být potřeba opravit
2. Restartuj server pro inicializaci TicketConfig
3. Přesuň `voidium_links.json` do `config/voidium/` (automatické při prvním spuštění)
4. Překontroluj ticket nastavení ve web panelu

### Nové konfigurovatelné možnosti
- Všechny ticket zprávy (6 zpráv)
- Ticket kategorie a support role
- Max tickety na uživatele
- Report channel ID pro statistiky

### Známé problémy
- Žádné známé kritické problémy

## 🙏 Poděkování

Děkujeme za používání Voidium Server Manager! Pokud narazíte na problémy nebo máte návrhy, neváhejte otevřít issue na GitHubu.

---

**Verze**: 1.3.7  
**Minecraft**: 1.21.1  
**NeoForge**: Compatible  
**Datum vydání**: 24.11.2025
