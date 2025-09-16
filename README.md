# VOIDIUM – SERVER MANAGER

Simple and powerful NeoForge server management: automated restarts, announcements, live performance metrics, and offline-mode skin restoration.

_Mod made with AI_

## 🔄 Restart System

*   Fixed-time, interval, delayed, and manual restarts
*   Boss bar countdown and structured warnings

## 📢 Announcements

*   Scheduled and manual broadcasts
*   Color codes, formatting, custom prefix, hot reload

## 📊 Monitoring & Utilities

*   TPS, MSPT, memory usage
*   Player list with ping
*   Server and mod info, public status

## 🧍 Offline-Mode Skin Restorer

*   Early join injection (no relog required)
*   Persistent JSON cache, configurable TTL
*   Manual refresh, auto-disabled in online mode

## ✅ Commands (Operators)

`/voidium restart <minutes>` · `/voidium announce <message>` · `/voidium players` · `/voidium memory` · `/voidium cancel` · `/voidium config` · `/voidium reload` · `/voidium skin <player>`  
Players: `/voidium status`

## 🔧 Technical

*   Minecraft: 1.21.1
*   Loader: NeoForge
*   Server-only (clients not required)
*   License: MIT
*   Lightweight & modular

## 📌 Notes

*   `skinCacheHours` below 1 is forced to 1
*   Expired cache entries refresh at next login
*   Safe in online mode (skin feature auto-skips)

**Professional server control, zero hassle.**
