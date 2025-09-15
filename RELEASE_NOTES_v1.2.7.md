# Voidium v1.2.7 - Release Notes

## ✅ Summary
Tato verze přináší okamžité načtení skinů v offline módu ještě před prvním broadcastem hráče a **persistentní diskovou cache** (24h TTL), takže opakovaná připojení hráčů už nezatěžují Mojang API.

## ✨ Novinky
- Early skin injection přes mixin do `PlayerList.placeNewPlayer` (žádný relog nutný)
- `skin-cache.json` (automaticky v `config/voidium/`) ukládá: uuid, value, signature, timestamp
- Fallback `SkinRestorer` zjednodušen – aktivní jen pokud early krok selže
- Odstraněny hacky (opakované ADD/REMOVE smyčky, přepínání gamemode)

## 🔧 Změny
- Stabilnější a rychlejší připojení hráče v offline režimu
- Menší síťová režie – většina hráčů po prvním joinu jede z cache
- Logy nyní jasně ukazují: cache hit / Mojang fetch / fallback

## 🗂 Cache Detaily
Soubor: `config/voidium/skin-cache.json`
Struktura:
```json
{
  "playername": {
    "uuid": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
    "value": "<base64 textures>",
    "signature": "<signature>",
    "cachedAt": 1737000000
  }
}
```
TTL: 24 hodin (po expiraci se automaticky refetchne).

## 🧪 Příkaz / Testování
1. Spusť server v offline módu
2. Připoj se jako nový hráč – skin se zobrazí hned
3. Zkontroluj `skin-cache.json`
4. Odpoj / znovu připoj – log ukáže `cache hit`

## 🛠 Možné Budoucí Kroky
- Konfigurovatelné TTL v `general.json`
- `/voidium skin clear <player|all>`
- Statistiky: počet cache hitů / misses

## ♻️ Upgrade
Není potřeba žádná manuální akce. Soubor cache se vytvoří automaticky.

---
Díky za používání Voidium! Máš návrh? Přidej issue na GitHub.
