# Voidium v2.0.0 Release Notes

## 🎉 Major Version Release!

Toto je major release s významnými vylepšeními web panelu, Discord integrace a TAB listu.

---

## 🌐 Web Control Panel Improvements

### 🎨 Role Prefix Editor - Vylepšení
- ✅ **Hex barvy v prefixech** - Místo mapování na nejbližší MC barvu se nyní použije přesná hex barva z Discord role (`&#RRGGBB` formát)
- ✅ **Automatická aktualizace prefixu** - Při změně role v dropdownu se prefix automaticky přegeneruje s barvou a názvem nové role
- ✅ **Ukládání všech polí** - Color a Priority se nyní správně ukládají spolu s prefixem a suffixem

### 📝 Vysvětlivky a Proměnné
- ✅ **Kompletní popisky** - Každé konfigurační pole má nyní vysvětlivku s dostupnými proměnnými
- ✅ **Proměnné pro PlayerList**: `%online%`, `%max%`, `%tps%`, `%playtime%`, `%time%`, `%memory%`
- ✅ **Proměnné pro jména**: `%rank_prefix%`, `%player_name%`, `%rank_suffix%`
- ✅ **Proměnné pro Discord**: `%player%`, `%user%`, `%message%`, `%code%`, `%max%`, `%uuid%`, `%count%`
- ✅ **Nápověda pro barevné kódy**: `&0-f` (legacy), `&#RRGGBB` (hex), `&l`, `&o`, `&n`, `&m`, `&r` (formátování)
- ✅ **Vylepšený styl popisků** - Fialový levý okraj, jemné pozadí, lepší čitelnost

### 🎯 UI/UX Vylepšení
- ✅ **Tmavé dropdown menu** - Select/option elementy mají nyní tmavé pozadí s čitelným textem
- ✅ **Vlastní šipka** - Fialová SVG šipka místo defaultní prohlížečové
- ✅ **Hover efekty** - Světlejší pozadí s fialovou barvou při najetí myší

---

## 👾 Discord Bot - Konfigurovatelné Zprávy

### Nové Konfigurační Pole
Všechny zprávy Discord bota jsou nyní konfigurovatelné přes web panel:

| Pole | Popis | Proměnné |
|------|-------|----------|
| `invalidCodeMessage` | Zpráva při neplatném kódu | - |
| `notLinkedMessage` | Zpráva pro nepropojeného uživatele | - |
| `alreadyLinkedSingleMessage` | Zpráva když je již propojený (1 účet) | `%uuid%` |
| `alreadyLinkedMultipleMessage` | Zpráva pro více propojených účtů | `%count%` |
| `unlinkSuccessMessage` | Zpráva při úspěšném odpojení | - |
| `wrongGuildMessage` | Zpráva pro špatný Discord server | - |
| `ticketCreatedMessage` | Zpráva při vytvoření ticketu | - |
| `ticketClosingMessage` | Zpráva při zavírání ticketu | - |
| `textChannelOnlyMessage` | Zpráva když příkaz vyžaduje textový kanál | - |

### Locale Preset
- ✅ Všechny nové zprávy mají CZ i EN překlady
- ✅ Funkce "Reset to Czech/English" zahrnuje nové zprávy

---

## 🐛 Bug Fixes

### Discord Bot
- ✅ **Null-safe gettery** - Opravena chyba `Content may not be null` při button interakci (ticket close)
- ✅ **Fallback hodnoty** - Všechny bot zprávy mají fallback na výchozí hodnotu pokud config neobsahuje klíč

### PlayerList (TAB)
- ✅ **Odstraněn spam v konzoli** - Odstraněny verbose debug logy z PlayerListManager

### Web Panel
- ✅ **Odstraněn spam v konzoli** - Odstraněny verbose debug logy z WebManager
- ✅ **Role prefix editor** - Opraveno ukládání color a priority polí

---

## 📦 Kompatibilita

- **Minecraft**: 1.21.1
- **NeoForge**: 21.1.208+
- **Java**: 21+

---

## ⬆️ Upgrade z v1.3.x

1. Nahraďte JAR soubor novou verzí
2. Restartujte server
3. (Volitelné) Použijte "Reset to Czech/English" v web panelu pro načtení nových překladů
4. Zkontrolujte a upravte nové Discord bot zprávy podle potřeby

---

## 📝 Změněné Soubory

- `DiscordConfig.java` - 9 nových konfiguračních polí + null-safe gettery
- `LocalePresets.java` - CZ a EN překlady pro nové zprávy
- `DiscordManager.java` - Použití konfigurovatelných zpráv
- `WebManager.java` - Vylepšení UI, popisky, dropdown styly, role prefix editor
- `PlayerListManager.java` - Odstranění debug logů
