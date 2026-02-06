---
layout: default
title: Plans & Roadmap (EN)
---

<div class="header-section">
  <h1>🚀 Plans & Roadmap</h1>
  <p class="subtitle">Voidium Project Development Overview</p>
</div>

<div class="row">
    <!-- PLANNED FUNCTIONS -->
    <div class="col-md-12 mb-4">
        <div class="section-card">
            <h2>🛠️ Planned Features (TODO)</h2>
            <p>The following features are planned for future versions.</p>
            
            <div class="feature-list">
                <h3>1) Web Control Interface v2</h3>
                <ul>
                    <li><strong>Discord OAuth</strong> – Login to panel using Discord account acces with specific role (no console token required).</li>
                    <li><strong>Live Console</strong> – Real-time server console monitoring directly in browser (WebSocket).</li>
                    <li><strong>Graphs & Visuals</strong> – History graphs for TPS, RAM, and Player Count in the dashboard.</li>
                </ul>

                <h3>2) Advanced Discord Integration</h3>
                <ul>
                    <li><strong>Auto Role Sync</strong> – Automatic Discord role updates based on MC rank progression (removes old role, adds new one on promotion).</li>
                </ul>

                <h3>3) Client & Rendering</h3>
                <ul>
                    <li><strong>Client-side RGB Colors & Emoji</strong> – Fix RGB color rendering (&#RRGGBB) in combination with emoji on client (currently emoji breaks text colors).</li>
                </ul>

                <h3>4) System Tools</h3>
                <ul>
                    <li><strong>Backup Manager</strong> – Manage, create and restore world backups via web interface.</li>
                    <li><strong>Maintenance Mode</strong> – Special maintenance mode with custom MoTD and user acces with specific discord role logic.</li>
                    <li><strong>Granular Event Logger</strong> – option to have Separate channels for different event types (deaths, commands (all), suspicious activity).</li>
                </ul>

                <h3>5) Monitoring & Performance</h3>
                <ul>
                    <li><strong>Performance Alerts</strong> – Discord notifications for low TPS, high RAM or CPU usage.</li>
                    <li><strong>Crash Reporter</strong> – Automatic crash report sending to Discord with details.</li>
                    <li><strong>Enhanced Daily Reports</strong> – Extend daily reports with best daily player (most time online) and average TPS.</li>
                </ul>

                <h3>6) Player & Admin Management</h3>
                <ul>
                    <li><strong>Player History</strong> – Track joins, quits, online time (with CSV export).</li>
                    <li><strong>AFK Manager</strong> – Automatic AFK player kicking after configured time.</li>
                    <li><strong>Temporary Bans/Mutes</strong> – Temporary punishments with automatic unban after expiration.</li>
                    <li><strong>Vanish Mode</strong> – Invisibility for admins (not in playerlist, nobody can see them).</li>
                </ul>
            </div>
        </div>
    </div>

    <!-- COMPLETED FUNCTIONS -->
    <div class="col-md-12">
        <div class="section-card completed-section">
            <h2>✅ Completed Features (Completed)</h2>
            <p>Features that are already successfully implemented and available in game.</p>

            <div class="feature-list">
                <h3>Discord Integration</h3>
                <ul class="checked-list">
                    <li><strong>Whitelist System</strong> – Discord & MC account linking, verification codes.</li>
                    <li><strong>Slash Commands</strong> – Modern Discord commands (<code>/link</code>, <code>/unlink</code>, <code>/ticket</code>).</li>
                    <li><strong>Chat Bridge</strong> – Two-way Game ↔ Discord communication.</li>
                    <li><strong>Webhook Chat</strong> – MC messages to Discord with player avatars via webhook.</li>
                    <li><strong>Status Embed</strong> – Automatic 'Live' server status message.</li>
                    <li><strong>Console Log</strong> – Streaming server console to a private channel.</li>
                    <li><strong>Topic Updater</strong> – Displaying stats (Players, TPS) in channel topic.</li>
                </ul>

                <h3>Statistics & Reports</h3>
                <ul class="checked-list">
                    <li><strong>Data Collection</strong> – Background server activity data collection.</li>
                    <li><strong>Daily Reports</strong> – Daily summary (Peak players) sent to Discord.</li>
                </ul>

                <h3>Ticket System</h3>
                <ul class="checked-list">
                    <li><strong>Discord Tickets</strong> – Player creates ticket via <code>/ticket</code> command.</li>
                    <li><strong>Channel Management</strong> – Automatic channel creation, permissions, and limits.</li>
                </ul>

                <h3>Vote System</h3>
                <ul class="checked-list">
                    <li><strong>NuVotifier Support</strong> – V1/V2 vote receiving.</li>
                    <li><strong>Offline Queue</strong> – Votes for offline players are saved and paid on join.</li>
                    <li><strong>Rewards</strong> – Configurable specific rewards (commands, items).</li>
                </ul>

                <h3>Core & Utilities</h3>
                <ul class="checked-list">
                    <li><strong>Web Config Editor</strong> – Basic HTTP server for editing configs (JSON).</li>
                    <li><strong>Auto-Rank</strong> – Promoting players based on playtime.</li>
                    <li><strong>Plugin Manager / Hot-Swap</strong> – Ability to enable/disable specific Voidium modules at runtime.</li>
                    <li><strong>Entity Cleaner</strong> – Automatic cleaning of ground items and entities.</li>
                    <li><strong>Skin Restorer</strong> – Skin fixing for offline-mode servers.</li>
                </ul>
            </div>
        </div>
    </div>
</div>
