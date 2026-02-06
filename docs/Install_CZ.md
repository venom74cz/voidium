---
layout: default
title: Instalace (CZ)
---

# 🧰 Instalace

<div class="hero">
  <p>Voidium je <strong>jeden jar</strong> pro všechny verze Minecraftu od <strong>1.21.1+</strong>. Na serveru běží na <strong>NeoForge dedikovaném serveru</strong>. Instalace na klienta je volitelná.</p>

  <div class="note"><strong>VERSION: 1.21.1+</strong> (1 jar pro 1.21.1+)</div>

  <h2>Rychlá navigace</h2>
  <div class="card-grid">
    <a class="card" href="#pozadavky">
      <div class="card-title"><span class="card-icon">✅</span>Požadavky</div>
      <div class="card-desc">NeoForge, Java 21, server</div>
    </a>
    <a class="card" href="#instalace-server">
      <div class="card-title"><span class="card-icon">🖥️</span>Instalace na server</div>
      <div class="card-desc">Stažení, mods/, první start</div>
    </a>
    <a class="card" href="#instalace-klient">
      <div class="card-title"><span class="card-icon">💻</span>Klient (volitelné)</div>
      <div class="card-desc">Chat UI + emoji + historie</div>
    </a>
    <a class="card" href="#prvni-spusteni">
      <div class="card-title"><span class="card-icon">⚙️</span>První spuštění</div>
      <div class="card-desc">Configy a storage</div>
    </a>
    <a class="card" href="#problemy">
      <div class="card-title"><span class="card-icon">🧯</span>Problémy</div>
      <div class="card-desc">Nejde načíst / chybí config</div>
    </a>
  </div>
</div>

## ✅ Požadavky {#pozadavky}

### Server

- NeoForge <strong>dedikovaný server</strong>
- Java <strong>21+</strong> (doporučeno Java 21 LTS)
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
  Voidium inicializuje serverové části pouze na <strong>dedikovaném serveru</strong>.
</div>

## 💻 Instalace na klienta (volitelné) {#instalace-klient}

Pokud chcete klientské funkce (moderní chat, emoji rendering, historie chatu), nainstalujte stejný jar i na klienta:

1. Nainstalujte NeoForge pro klient profil
2. Zkopírujte <code>voidium-*.jar</code> do:
   - Linux: <code>~/.minecraft/mods/</code>
   - Windows: <code>%APPDATA%\.minecraft\mods\</code>
3. Spusťte hru s NeoForge

## ⚙️ První spuštění: co se vytvoří {#prvni-spusteni}

Po prvním startu serveru typicky vznikne:

- <code>config/voidium/general.json</code> (globální přepínače modulů: Discord/Web/Stats/…)
- <code>config/voidium/storage/</code> (persistentní runtime data)

Voidium umí při startu automaticky přesunout starší storage soubory do <code>config/voidium/storage/</code> (pokud existují).

## 🔁 Aktualizace {#aktualizace}

1. Stop server
2. Záloha <code>config/voidium/</code>
3. Vyměňte jar v <code>mods/</code>

Konfigurace i data zůstávají v <code>config/voidium/</code>. Pokud se změní interní layout storage, Voidium provede migraci do <code>config/voidium/storage/</code> automaticky.

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

