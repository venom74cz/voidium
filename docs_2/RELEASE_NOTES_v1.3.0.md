# Voidium Server Manager 1.3.0 Release Notes

## ✨ What's New in 1.3.0
- **NuVotifier-compatible vote listener** – accepts both token-based V2 packets and legacy RSA V1 payloads simultaneously, with automatic handshake and signature validation.
- **Autonomous configuration bootstrap** – `config/voidium/votes.json` is generated on first launch, including RSA key pair paths, a random 16-character shared secret, and ready-made reward command placeholders.
- **Robust vote logging** – plain text log (`votes.log`) and NDJSON archive (`votes-history.ndjson`) now capture every successful vote for analytics.
- **Admin safety features** – optional OP notifications on listener failure, full payload diagnostics in the console, and strict payload length / signature validation to block malformed votes.

## ✅ Upgrade Checklist
1. **Deploy the new JAR** (see build instructions below).
2. Start the server once to generate or update `config/voidium/votes.json` and RSA key files.
3. Share `votifier_rsa_public.pem` or the auto-generated shared secret with vote portals (keep the private key & secret confidential!).
4. Customize the `commands` array to match your reward system.
5. (Optional) Adjust logging destinations or disable the NDJSON archive if not required.

## 📂 Key Files
| File | Purpose |
| --- | --- |
| `config/voidium/votes.json` | Master vote listener configuration; contains listener host/port, RSA paths, shared secret, reward commands, and logging toggles. |
| `config/voidium/votifier_rsa.pem` | RSA private key (generated if missing). |
| `config/voidium/votifier_rsa_public.pem` | Public key to upload to legacy V1 vote sites. |
| `config/voidium/votes.log` | Tab-separated text log of successful votes. |
| `config/voidium/votes-history.ndjson` | Optional JSONL archive for external ingestion. |

## 🔁 Behaviour Notes
- When a shared secret is present, the listener follows the official NuVotifier V2 handshake (`VOTIFIER 2 <challenge>`) and responds with JSON status objects.
- Legacy RSA payloads (V1 style) remain fully functional and are validated with the generated key pair.
- Malformed packets or signature mismatches are logged and announced to operators (if enabled) but do not impact other votes.

## 🛠 Build & Deploy
```powershell
# from repository root
./gradlew.bat clean build -x test
```
The distributable JAR will be located at `build/libs/voidium-1.3.0.jar`.

---
Need help wiring the vote listener to your portal? Check `docs/votifier-plan.md` for deeper implementation notes and future roadmap ideas.
