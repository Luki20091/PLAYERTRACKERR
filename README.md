<div align="center">

# ━━━━━━━━━━━━━━━━━━━
## **PLAYERTRACKER**
### *Track Players Using a Compass — Smart, Simple, Survival-Friendly*
# ━━━━━━━━━━━━━━━━━━━

**Minecraft:** 1.20 – 1.21.5  
**Java:** 17+  
**Dependencies:** *Optional* — Vault (for charging tracking fees)

![PlayerTracker Banner](https://i.imgur.com/Jj663S1.png)

</div>

---

## ✅ Overview

**PlayerTracker** lets players track another player using a **compass**, updating in real-time as the target moves.  
Designed for **SMPs, PvP hunts, bounty servers, and survival gameplay** — with clean, efficient performance and easy configuration.

The plugin ensures:
- Fair tracking rules
- Optional cost to track (Vault support)
- Item-based control (hold or possess a compass)
- Distance and world restrictions
- Smooth tracking updates 

---

## ⭐ Features

| Feature | Description |
|--------|-------------|
| 🎯 **Compass Tracking** | Track any player with an updating compass pointer. |
| 🧭 **Nearest Player Quick Track** | Right-click compass → instantly track nearest player. |
| ❌ **Drop Compass to Stop** | Drop or switch items to pause/stop tracking (configurable). |
| 🔒 **Bypass Permission** | Certain players can be untrackable (`pt.bypass`). |
| 💰 **Optional Tracking Fee** | Charge players using **Vault economy**. |
| 🌍 **Distance + World Checks** | Automatically stops if player is too far or changes worlds. |
| ⚙️ **Highly Configurable** | All messages, rules, and amounts configurable. |

---

## 📦 Commands

| Command                 | Description                                | Permission |
|-------------------------|--------------------------------------------|------------|
| `/track <player>`       | Start tracking a specific player           | `pt.track` |
| `/cleartarget`          | Clear your current target                  | `pt.cleartarget` 
| `/cleartarget <player>` | Clear another players target               | `pt.cleartarget.other` 
| `/trackers`             | GUI showing all players currently tracking | `pt.trackers` 
| `/pt reload`            | Reload the config.yml                      | `pt.reload` 
| `/playertracker`        | Main help command for PlayerTracker        | `NONE` 
| *(Compass Right-Click)* | Track nearest player (if enabled)          | `pt.track` |
| *(Compass Left-Click)*  | Stop tracking                              | `pt.track` |

---

## 🔐 Additional Permissions

| Permission       | Effect                                  |
|------------------|-----------------------------------------|
| `pt.bypass`      | Player **cannot** be tracked            |
| `pt.notify`      | Target gets notified when being tracked |

---

## ⚙️ Configuration (config.yml)

```yaml
# ===================================================================
#  PlayerTracker — Main Configuration File
#  Created by QuaccOnCracc (2025)
#  For support, visit the SpigotMC resources page
# ===================================================================
#
#  Formatting:
#     - Use & for color codes.
#     - HEX colors (#RRGGBB) are supported.
#     - Variables: {prefix}, {target}, {tracker}, {distance}, {amount}, {item}
#
# ===================================================================


# ==============================================================
#  ► Tracking Behaviour
# ==============================================================

# Right-clicking a compass automatically tracks the NEAREST player.
track-nearest: true

# Maximum distance (in blocks) a target can be from the tracker.
tracking-distance: 10000


# ==============================================================
#  ► Compass Settings
# ==============================================================

compass:
  # If true, player must HOLD the compass.
  # If false, compass only needs to be somewhere in their inventory.
  require-compass-hand: true

  # Cooldown after clicking a compass (in seconds).
  cooldown-seconds: 5

  # Sent when clicking the compass while on cooldown.
  cooldown-message: "&cYou must wait &f{time}s &cbefore searching again!"


# ==============================================================
#  ► Tracking Fees
# ==============================================================

tracking-fee-item:
  enabled: true
  material: "DIAMOND"
  amount: 1
  item-taken-message: "&cRequired tracking fee of &f{amount} {item} &ctaken"

tracking-fee-vault:
  enabled: false
  amount: 0
  withdraw-fee-message: "&cWithdraw tracking fee of &f{amount} &cfrom your account!"


# ==============================================================
#  ► Messages
# ==============================================================

# Global plugin prefix
plugin-prefix: "&a&lTracker &8» "

# No nearby players to track
no-close-player: "&cNo nearby players found to track!"

# Target left max distance
outside-tracking-distance: "&f{target} &chas moved too far away to track!"

# Missing required item
insufficient-fee-item: "&cMissing required tracking fee: &f{amount} {item}"

# Missing Vault currency
insufficient-fee-vault: "&cYou need at least &f{amount}&c to track this player!"

# Notify target of being tracked
notify-target: "&cWARNING: You are being tracked by &f{tracker}"

# Attempting to track yourself
track-self: "&cYou cannot track yourself!"

# Generic no-permission
no-permission: "&cYou do not have permission to do that."

# Config reload success
reloaded-config-message: "&aConfiguration reloaded successfully!"

# Actionbar tracking display (sent repeatedly)
tracking-message: "&aTracking &e{target} &7| Distance: &b{distance}m"

# No player supplied to /track
player-not-specified: "&cPlease specify a player!"

# Target not online
player-not-online: "&cThat player is not online!"

# Target is in another world
not-in-world: "&f{target} &cis in another world — cannot track them!"

# Target changed worlds while being tracked
changed-world: "&cYour target changed worlds — tracking stopped!"

# Used /track without having a compass
no-compass: "&cYou need a compass to use &f/track&c!"

# Player lost their compass while tracking
tracking-stopped-no-compass: "&cCompass missing — tracking stopped!"

# Compass must be held
compass-not-in-hand: "&cYou must hold a compass to track!"

# Player is not tracking anyone
not-tracking-player: "&cYou are not currently tracking anyone."

# Target cannot be tracked due to bypass permission
tracker-bypass-message: "&cThis player cannot be tracked."

# Tracking stopped by admin or server event
forced-stop: "&cTracking of &f{target} &chas been forcefully stopped."

# Player stopped tracking themselves (/cleartarget)
self-stop: "&aYou have stopped tracking your target."

# Target logged out
target-left: "&cStopped tracking &f{target}&c — they left the server."
```

🛠 Installation
Place the PlayerTracker.jar into your /plugins/ folder.

(Optional) Install Vault & an Economy plugin (EssentialsX, CMI, etc.).

Restart your server.

Edit config.yml to your liking.

Enjoy!

📸 Screenshots / Media 
nginx
Copy code
Place example images or GIFs here.
🤝 Support & Suggestions
Have ideas? Need help? Want a custom feature?

• Open an Issue on GitHub
• Or message via Spigot resource discussion

<div align="center">

</div>