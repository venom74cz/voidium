---
layout: default
title: Plány a Roadmap (CZ)
---

<div class="header-section">
  <h1>🚀 Plány a Roadmap</h1>
  <p class="subtitle">Přehled vývoje projektu Voidium</p>
</div>

<div class="row">
    <!-- PLÁNOVANÉ FUNKCE -->
    <div class="col-md-12 mb-4">
        <div class="section-card">
            <h2>🛠️ Plánované funkce (TODO)</h2>
            <p>Následující funkce jsou v plánu pro budoucí verze.</p>
            
            <div class="feature-list">
                <h3>1) Otevřené follow-upy web panelu</h3>
                <ul>
                    <li><strong>Discord OAuth</strong> – Přihlášení do panelu pomocí Discord účtu s určitou rolí (bez nutnosti tokenu z konzole).</li>
                    <li><strong>🔧 Persistentní AI historie (WIP)</strong> – Ukládání historie konverzací admin AI napříč sezeními pro kontinuitu.</li>
                    <li><strong>🔧 Statistiky ticketů v dashboardu (WIP)</strong> – Analytika a statistiky pro ticketový systém (doba odezvy, míra vyřešení, objem).</li>
                    <li><strong>🔧 Výběr ranku pro PLAYTIME režim (WIP)</strong> – Když je režim přístupu hráčů k AI nastaven na PLAYTIME, zobrazit dropdown s nakonfigurovanými ranky, aby admin mohl vybrat z existujících definic místo ručního zadávání hodin.</li>
                </ul>

                <h3>2) Pokročilá Discord Integrace</h3>
                <ul>
                    <li><strong>Auto Role Sync</strong> – Automatická aktualizace Discord rolí podle MC ranku za odehrané hodiny (při povýšení smaže předchozí roli a přidá novou).</li>
                </ul>

                <h3>3) Klient & Rendering</h3>
                <ul>
                    <li><strong>Client-side RGB Colors & Emoji</strong> – Oprava zobrazování RGB barev (&#RRGGBB) v kombinaci s emoji na klientovi (nyní emoji ničí barvy textu).</li>
                </ul>

                <h3>4) Systémové nástroje</h3>
                <ul>
                    <li><strong>Backup Manager</strong> – Správa, vytváření a obnova záloh světa přes webové rozhraní.</li>
                    <li><strong>Granulární Event Logger</strong> – Možnost mít oddělené kanály pro různé typy událostí (smrti, příkazy (všechny), podezřelá aktivita).</li>
                </ul>

                <h3>5) Monitoring & Performance</h3>
                <ul>
                    <li><strong>Performance Alerts</strong> – Upozornění na Discord při nízkém TPS, vysoké RAM nebo CPU.</li>
                    <li><strong>Crash Reporter</strong> – Automatické odesílání crash reportů na Discord s detaily.</li>
                    <li><strong>Enhanced Daily Reports</strong> – Rozšíření denních reportů o nejlepšího denního hráče (čas online) a průměr TPS.</li>
                </ul>

                <h3>6) Player & Admin Management</h3>
                <ul>
                    <li><strong>Player History</strong> – Sledování připojení, odpojení, času online (s export do CSV).</li>
                    <li><strong>AFK Manager</strong> – Automatické kickování AFK hráčů po nastaveném čase.</li>
                    <li><strong>Temporary Bans/Mutes</strong> – Dočasné tresty s automatickým unban po vypršení.</li>
                    <li><strong>Vanish Mode</strong> – Neviditelnost pro adminy (nejsou v playerlistu, nikdo je nevidí).</li>
                </ul>
            </div>
        </div>
    </div>

    <!-- DOKONČENÉ FUNKCE -->
    <div class="col-md-12">
        <div class="section-card completed-section">
            <h2>✅ Dokončené funkce (Completed)</h2>
            <p>Funkce, které jsou již úspěšně implementovány a dostupné ve hře.</p>

            <div class="feature-list">
                <h3>Web Control Panel v2</h3>
                <ul class="checked-list">
                    <li><strong>React + Vite Web Panel</strong> – Kompletní SPA dashboard zabudovaný do JAR místo starého basic editoru.</li>
                    <li><strong>Config Studio</strong> – Vizualní editace konfigurace s preview, diffem, apply flow a audit logem.</li>
                    <li><strong>Live Console & Grafy</strong> – Živý výstup konzole, feedy a historie výkonu přímo v panelu.</li>
                    <li><strong>Maintenance Mode</strong> – Web toggle, banner v dashboardu a blokace připojení pro ne-OP hráče.</li>
                </ul>

                <h3>Discord Integrace</h3>
                <ul class="checked-list">
                    <li><strong>Whitelist System</strong> – Propojení Discord a MC účtů, ověřovací kód.</li>
                    <li><strong>Slash Commands</strong> – Moderní Discord příkazy (<code>/link</code>, <code>/unlink</code>, <code>/ticket</code>).</li>
                    <li><strong>Chat Bridge</strong> – Obousměrná komunikace Hra ↔ Discord.</li>
                    <li><strong>Webhook Chat</strong> – Zprávy z MC na Discord s avatary hráčů přes webhook.</li>
                    <li><strong>Status Embed</strong> – Automatická 'Live' zpráva se stavem serveru.</li>
                    <li><strong>Console Log</strong> – Streamování serverové konzole do privátního kanálu.</li>
                    <li><strong>Topic Updater</strong> – Zobrazování statistik (hráči, TPS) v popisu kanálu.</li>
                </ul>

                <h3>Statistiky a Reporty</h3>
                <ul class="checked-list">
                    <li><strong>Data Collection</strong> – Sběr dat o aktivitě serveru na pozadí.</li>
                    <li><strong>Daily Reports</strong> – Každodenní souhrn (Peak hráčů) odesílaný na Discord.</li>
                </ul>

                <h3>Ticket System</h3>
                <ul class="checked-list">
                    <li><strong>Discord Tickets</strong> – Hráč vytvoří ticket příkazem <code>/ticket</code>.</li>
                    <li><strong>Channel Management</strong> – Automatické vytváření kanálů, nastavování práv a limitů.</li>
                </ul>

                <h3>Vote System</h3>
                <ul class="checked-list">
                    <li><strong>NuVotifier Support</strong> – Příjem V1/V2 hlasů.</li>
                    <li><strong>Offline Queue</strong> – Hlasy pro offline hráče se uloží a vyplatí po připojení.</li>
                    <li><strong>Rewards</strong> – Konfigurovatelné odměny (příkazy, itemy).</li>
                </ul>

                <h3>Jádro a Utility</h3>
                <ul class="checked-list">
                    <li><strong>Auto-Rank</strong> – Povyšování hráčů na základě odehraného času.</li>
                    <li><strong>Plugin Manager / Hot-Swap</strong> – Možnost vypínat a zapínat jednotlivé moduly Voidium za běhu.</li>
                    <li><strong>Entity Cleaner</strong> – Automatické čištění itemů a entit na zemi.</li>
                    <li><strong>Skin Restorer</strong> – Oprava skinů pro servery v offline módu.</li>
                </ul>
            </div>
        </div>
    </div>
</div>
