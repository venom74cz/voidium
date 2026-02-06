---
layout: default
title: Instalace (CZ)
---

# 📦 Instalace Voidium

## Požadavky

### Server
- **Minecraft**: 1.21.1
- **Mod Loader**: NeoForge 21.1.x nebo novější
- **Java**: Java 21+ (doporučeno Java 21 LTS)
- **OS**: Linux, Windows, nebo macOS
- **RAM**: Minimálně 2GB pro server + 512MB pro mod

### Client (volitelné)
- **Minecraft**: 1.21.1
- **Mod Loader**: NeoForge 21.1.x
- **Java**: Java 21+

## Instalace na server

### Krok 1: Stažení

1. Stáhněte nejnovější verzi Voidium z [GitHub Releases](https://github.com/yourname/voidium/releases)
---
layout: default
title: Instalace (CZ)
---

# 🧰 Instalace

<div class="hero">
  <p>Voidium běží na <strong>NeoForge dedikovaném serveru</strong>. Instalace na klienta je volitelná (přidává hlavně UI/chat funkce).</p>

  <h2>Rychlá navigace</h2>
  <div class="card-grid">
    <a class="card" href="#pozadavky">
      <div class="card-title"><span class="card-icon">✅</span>Požadavky</div>
      <div class="card-desc">Java, NeoForge, dedicated server</div>
    </a>
    <a class="card" href="#instalace-server">
      <div class="card-title"><span class="card-icon">🖥️</span>Instalace na server</div>
      <div class="card-desc">Stažení, mods/, první start</div>
    </a>
    <a class="card" href="#instalace-klient">
      <div class="card-title"><span class="card-icon">💻</span>Klient (volitelné)</div>
      <div class="card-desc">Moderní chat + emoji + historie</div>
    </a>
    <a class="card" href="#prvni-spusteni">
      <div class="card-title"><span class="card-icon">⚙️</span>První spuštění</div>
      <div class="card-desc">Generované configy & storage</div>
    </a>
    <a class="card" href="#aktualizace">
      <div class="card-title"><span class="card-icon">🔁</span>Aktualizace</div>
      <div class="card-desc">Bezpečný update + migrace dat</div>
    </a>
    <a class="card" href="#problemy">
      <div class="card-title"><span class="card-icon">🧯</span>Problémy</div>
      <div class="card-desc">Nejčastější chyby při instalaci</div>
    </a>
  </div>
</div>

## ✅ Požadavky {#pozadavky}

<div class="note">
  <strong>Bez hardcoded verzí:</strong> přesná kompatibilita Minecraft/NeoForge záleží na tom, jaký jar jste stáhli.
  Autoritativní informace najdete v <code>META-INF/mods.toml</code> uvnitř jaru (nebo v repozitáři v <code>mods.toml</code>).
</div>

### Server

- NeoForge <strong>dedikovaný server</strong>
- Java <strong>21+</strong> (doporučeno)
- Složka serveru s <code>mods/</code>

### Klient (volitelné)

- NeoForge klient odpovídající verzi hry
- Java 21+

## 🖥️ Instalace na server {#instalace-server}

### 1) Stažení

- Stáhněte release jar z:
  - https://github.com/venom74cz/voidium/releases

### 2) Umístění do <code>mods/</code>

Vložte jar přímo do <code>mods/</code>.

```bash
# Linux/macOS
cd /cesta/k/serveru
mkdir -p mods
cp voidium-*.jar mods/
```

### 3) První start

Po prvním spuštění Voidium vytvoří konfiguraci do:

```
config/voidium/
```

<div class="note">
  Pokud se složka <code>config/voidium/</code> nevytvoří, zkontrolujte, že jste spustili <strong>dedikovaný server</strong>.
  Voidium inicializuje serverové manažery pouze na dedikovaném serveru.
</div>

## 💻 Instalace na klienta (volitelné) {#instalace-klient}

Pokud chcete klientské funkce (moderní chat, emoji rendering, historie chatu), nainstalujte stejný jar i na klienta:

1. Nainstalujte NeoForge pro klient profil
2. Zkopírujte <code>voidium-*.jar</code> do:
   - Linux: <code>~/.minecraft/mods/</code>
   - Windows: <code>%APPDATA%\.minecraft\mods\</code>
3. Spusťte hru s NeoForge

## ⚙️ První spuštění: co se vytvoří {#prvni-spusteni}

Po prvním startu serveru vznikne:

- <code>config/voidium/general.json</code> (globální přepínače modulů: Discord/Web/Stats/…)
- Konfigurace modulů (např.): <code>discord.json</code>, <code>web.json</code>, <code>stats.json</code>, <code>ranks.json</code>, …
- <code>config/voidium/storage/</code> pro persistentní data (linky, vote queue, cache skinů, history soubory)

Voidium umí na startu automaticky přesunout starší storage soubory do <code>config/voidium/storage/</code> (pokud existují).

## 🔁 Aktualizace {#aktualizace}

Doporučený bezpečný postup:

1. Stop server
2. Záloha <code>config/voidium/</code>
3. Nahraďte starý jar v <code>mods/</code> novým
4. Spusťte server

Konfigurace i data zůstávají v <code>config/voidium/</code>. Pokud se mění interní layout storage, Voidium provede migraci do <code>config/voidium/storage/</code> automaticky.

## 🧯 Řešení problémů {#problemy}

### Voidium se nenačte

- Ověřte, že používáte <strong>NeoForge</strong> (ne Forge/Fabric)
- Ověřte Java 21+: <code>java -version</code>
- Jar musí být přímo v <code>mods/</code> (ne v podsložce)

### Konfigurace se nevytvoří

- Spusťte <strong>dedikovaný server</strong>
- V logu hledejte <code>VOIDIUM - INTELLIGENT SERVER CONTROL is loading...</code>

## Další kroky

- [Rychlý start](QuickStart_CZ.html)
- [Konfigurace](Config_CZ.html)
- [Discord setup](Discord_CZ.html)

