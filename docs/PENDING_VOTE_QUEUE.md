# Pending Vote Queue - Usage Examples

## Basic Workflow

### Player Votes While Offline
```
1. Player "Steve" is NOT online
2. Vote arrives from "minecraft-mp.com"
3. VoteListener detects Steve is offline
4. Vote is added to pending-votes.json:
   {
     "username": "Steve",
     "serviceName": "minecraft-mp.com",
     "address": "192.168.1.100",
     "timestamp": 1697040000000,
     "queuedAt": 1697040001000
   }
5. Vote is logged normally to votes.log
```

### Player Logs In
```
1. Steve connects to server
2. VoteManager.onPlayerLogin() is triggered
3. System checks: pendingQueue.hasPendingVotes("Steve") → true
4. Retrieves all pending votes for Steve
5. Displays: "§8[§bVoidium§8] §aYou have §e2§a pending vote rewards!"
6. Executes all reward commands for each vote
7. Displays: "§8[§bVoidium§8] §aAll pending vote rewards delivered!"
8. Removes votes from queue and saves
```

## Admin Commands

### Check Total Pending Votes
```
/voidium votes pending
→ "§8[§bVoidium§8] §fTotal pending votes: §e5"
```

### Check Specific Player
```
/voidium votes pending Steve
→ "§8[§bVoidium§8] §aPlayer §eSteve§a has §e2§a pending votes"

/voidium votes pending Alex
→ "§8[§bVoidium§8] §7Player §eAlex§7 has no pending votes"
```

### Clear All Pending (Emergency)
```
/voidium votes clear
→ "§8[§bVoidium§8] §aCleared §e5§a pending votes"
```

## File Structure

### pending-votes.json Example
```json
[
  {
    "username": "Steve",
    "serviceName": "minecraft-mp.com",
    "address": "192.168.1.100",
    "timestamp": 1697040000000,
    "queuedAt": 1697040001000
  },
  {
    "username": "Steve",
    "serviceName": "planetminecraft.com",
    "address": "192.168.1.100",
    "timestamp": 1697041000000,
    "queuedAt": 1697041001000
  },
  {
    "username": "Alex",
    "serviceName": "minecraft-mp.com",
    "address": "10.0.0.50",
    "timestamp": 1697042000000,
    "queuedAt": 1697042001000
  }
]
```

## Configuration

### votes.json - Logging Section
```json
{
  "logging": {
    "voteLog": true,
    "voteLogFile": "votes.log",
    "archiveJson": true,
    "archivePath": "votes-history.ndjson",
    "notifyOpsOnError": true,
    "pendingQueueFile": "pending-votes.json"
  }
}
```

## Thread Safety

The `PendingVoteQueue` uses:
- **ReentrantLock** for synchronization
- **Auto-save** after every enqueue/dequeue
- **Atomic operations** for count checks

Safe for:
- Multiple vote listener threads
- Concurrent player logins
- Admin commands during vote processing

## Logging Examples

### Offline Vote Received
```
[INFO] Vote received from minecraft-mp.com for player Steve
[INFO] Player Steve is offline, vote queued for later delivery
[INFO] Queued vote for offline player: Steve from minecraft-mp.com
```

### Player Login with Pending Votes
```
[INFO] Player Steve logged in with 2 pending vote(s)
[INFO] Delivering pending vote reward to Steve from minecraft-mp.com
[INFO] Delivering pending vote reward to Steve from planetminecraft.com
[INFO] Dequeued 2 pending vote(s) for player: Steve
```

### Admin Commands
```
[INFO] Admin checked pending votes: 5 total
[INFO] Cleared 5 pending vote(s)
```

## Edge Cases Handled

1. **Player never logs in** → Votes stay in queue indefinitely (no expiration)
2. **Server crash** → Queue persists in JSON file, loaded on restart
3. **Corrupted JSON** → Logs error, starts with empty queue
4. **Concurrent access** → Thread-safe locks prevent data corruption
5. **Duplicate votes** → Each vote processed independently (no deduplication)

## Performance

- **Memory:** O(n) where n = pending votes
- **Lookup:** O(n) linear search by username
- **File I/O:** Synchronized write on every change
- **Typical load:** <100 pending votes = negligible impact

## Best Practices

1. **Monitor queue size** regularly with `/voidium votes pending`
2. **Don't clear queue** unless absolutely necessary (lost rewards)
3. **Check logs** if players report missing rewards
4. **Backup** pending-votes.json during maintenance
5. **Test** with alt account before production deployment
