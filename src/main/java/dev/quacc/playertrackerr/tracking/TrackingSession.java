package dev.quacc.playertrackerr.tracking;

import dev.quacc.playertrackerr.config.ConfigOption;
import dev.quacc.playertrackerr.config.ConfigOptionsManager;
import dev.quacc.playertrackerr.tracking.helper.CompassHelper;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.Location;
import dev.quacc.playertrackerr.PlayerTrackerr;

import java.util.Map;
import java.util.UUID;

public class TrackingSession {

    private final UUID trackerId;
    private final UUID targetId;
    private final ConfigOptionsManager config;
    private final CompassHelper compassHelper;
    private final String trackedItemId;
    private long lastCompassUpdate = 0L;
    // temporary pause flag (e.g., while player is interacting with inventory)
    private volatile boolean paused = false;
    // Last seconds-until-update value sent to the player's action bar — prevents spamming the action bar
    private int lastSentRemaining = -1;
    // Distance caching to respect a separate distance update cooldown
    private long lastDistanceUpdate = 0L;
    private int cachedDistance = -1;
    // timestamp of last durability consumption to avoid double-consume
    private long lastConsumedTime = 0L;

    public TrackingSession(UUID trackerId, UUID targetId, ConfigOptionsManager config, String trackedItemId) {
        this.trackerId = trackerId;
        this.targetId = targetId;
        this.config = config;
        this.compassHelper = new CompassHelper(config);
        this.trackedItemId = trackedItemId;
    }

    public void setPaused(boolean value) {
        boolean old = this.paused;
        this.paused = value;
        try {
            if (config.getBoolean(dev.quacc.playertrackerr.config.ConfigOption.DEBUG) && old != value) {
                var plugin = JavaPlugin.getPlugin(PlayerTrackerr.class);
                var player = plugin.getServer().getPlayer(trackerId);
                String name = player != null ? player.getName() : trackerId.toString();
                plugin.getLogger().info("[PT Debug] Session for " + name + " paused=" + value);
            }
        } catch (Throwable ignored) {}
    }

    public boolean isPaused() {
        return this.paused;
    }

    public UUID getTrackerId() {
        return this.trackerId;
    }

    public UUID getTargetId() {
        return this.targetId;
    }

    public String getTrackedItemId() {
        return this.trackedItemId;
    }

    public boolean validateAndUpdate(JavaPlugin plugin, Player tracker, Player target) {
        if (!arePlayersOnlineAndNonNull(tracker, target)) return false;
        // If the target has bypass permission, stop tracking and notify the tracker.
        if (target != null && target.hasPermission("pt.bypass")) {
            tracker.sendMessage(config.format(dev.quacc.playertrackerr.config.ConfigOption.TRACKER_BYPASS_MESSAGE));
            return false;
        }
        if (!withinDistance(tracker, target)) return false;

        return handleCompassState(tracker, target, plugin);
    }



    private boolean arePlayersOnlineAndNonNull(Player tracker, Player target) {
        return tracker != null && target != null && tracker.isOnline() && target.isOnline();
    }

    private boolean withinDistance(Player tracker, Player target) {
        final int max = config.getInt(ConfigOption.TRACKING_DISTANCE);
        return tracker.getLocation().distanceSquared(target.getLocation()) <= (max * max);
    }

    private boolean handleCompassState(Player tracker, Player target, JavaPlugin plugin) {
        if (this.isPaused()) return true; // pause: don't update but keep session alive

        // If this session is tracking a specific item, but that item no longer
        // exists in the player's inventory, stop the session rather than
        // continuing with a different compass that won't consume durability.
        if (this.trackedItemId != null) {
            try {
                boolean found = false;
                NamespacedKey uniqueKey = new NamespacedKey(plugin, "pt_unique");
                var inv = tracker.getInventory();
                for (int i = 0; i < inv.getSize(); i++) {
                    var it = inv.getItem(i);
                    if (it == null) continue;
                    var m = it.getItemMeta();
                    if (m == null) continue;
                    var pdc = m.getPersistentDataContainer();
                    if (pdc.has(uniqueKey, PersistentDataType.STRING)) {
                        var id = pdc.get(uniqueKey, PersistentDataType.STRING);
                        if (this.trackedItemId.equals(id)) { found = true; break; }
                    }
                }
                if (!found) {
                    tracker.spigot().sendMessage(
                            new ComponentBuilder(
                                    config.format(dev.quacc.playertrackerr.config.ConfigOption.TRACKING_STOPPED_NO_COMPASS)
                            ).create()
                    );
                    return false;
                }
            } catch (Throwable ignored) {}
        }

        return switch (compassHelper.validate(tracker)) {
            case STOP -> {
                tracker.spigot().sendMessage(
                        new ComponentBuilder(
                                config.format(ConfigOption.TRACKING_STOPPED_NO_COMPASS)
                        ).create()
                );
                yield false;
            }

            case PAUSE -> {
                yield true;
            }

            case UPDATE -> {
                final boolean compassUpdated = shouldUpdateCompass();
                final boolean distanceUpdated = shouldUpdateDistance();

                if (compassUpdated) updateHeldCompass(tracker, target.getLocation());

                if (distanceUpdated) updateCachedDistance(tracker, target);
                // If configured, consume durability when a search actually triggers
                if (config.getBoolean(dev.quacc.playertrackerr.config.ConfigOption.COMPASS_CONSUME_DURABILITY) && (compassUpdated || distanceUpdated)) {
                    try { if (config.getBoolean(dev.quacc.playertrackerr.config.ConfigOption.DEBUG)) JavaPlugin.getPlugin(PlayerTrackerr.class).getLogger().info("[PT Debug] About to call consumeDurability (compassUpdated=" + compassUpdated + ",distanceUpdated=" + distanceUpdated + ") for " + tracker.getName()); } catch (Throwable ignored) {}
                    consumeDurability(tracker);
                }

                sendHUD(tracker, target, distanceUpdated ? 0 : getRemainingDistanceSeconds());
                yield true;
            }
        };
    }

    private boolean shouldUpdateDistance() {
        final boolean enabled = config.getBoolean(ConfigOption.COMPASS_UPDATE_COOLDOWN_ENABLED);
        if (!enabled) {
            lastDistanceUpdate = System.currentTimeMillis();
            return true;
        }

        final int seconds = config.getInt(ConfigOption.COMPASS_UPDATE_COOLDOWN_SECONDS);
        final long now = System.currentTimeMillis();
        if (now - lastDistanceUpdate >= seconds * 1000L) {
            lastDistanceUpdate = now;
            return true;
        }
        return false;
    }

    private int getRemainingDistanceSeconds() {
        final boolean enabled = config.getBoolean(ConfigOption.COMPASS_UPDATE_COOLDOWN_ENABLED);
        if (!enabled) return 0;

        final int seconds = config.getInt(ConfigOption.COMPASS_UPDATE_COOLDOWN_SECONDS);
        final long now = System.currentTimeMillis();
        final long elapsed = now - lastDistanceUpdate;
        final long remainingMs = Math.max(0L, seconds * 1000L - elapsed);
        return (int) ((remainingMs + 999L) / 1000L);
    }

    private void updateCachedDistance(Player tracker, Player target) {
        this.cachedDistance = (int) tracker.getLocation().distance(target.getLocation());
    }

    

    private boolean shouldUpdateCompass() {
        final boolean enabled = config.getBoolean(ConfigOption.COMPASS_UPDATE_COOLDOWN_ENABLED);
        if (!enabled) {
            lastCompassUpdate = System.currentTimeMillis();
            return true;
        }

        final int seconds = config.getInt(ConfigOption.COMPASS_UPDATE_COOLDOWN_SECONDS);
        final long now = System.currentTimeMillis();
        if (now - lastCompassUpdate >= seconds * 1000L) {
            lastCompassUpdate = now;
            return true;
        }
        return false;
    }

    public void updateCompassIfAllowed(Player tracker, Player target) {
        if (shouldUpdateCompass()) {
            updateHeldCompass(tracker, target.getLocation());
            // When we actually update the compass, send HUD immediately with 0 seconds remaining
            if (config.getBoolean(dev.quacc.playertrackerr.config.ConfigOption.COMPASS_CONSUME_DURABILITY)) {
                try { if (config.getBoolean(dev.quacc.playertrackerr.config.ConfigOption.DEBUG)) JavaPlugin.getPlugin(PlayerTrackerr.class).getLogger().info("[PT Debug] updateCompassIfAllowed: calling consumeDurability for " + tracker.getName()); } catch (Throwable ignored) {}
                consumeDurability(tracker);
            }
            sendHUD(tracker, target, 0);
            lastSentRemaining = 0;
        }
    }

    // Update the player's compass target only (no HUD, no durability consumption).
    // Used when we want to avoid overwriting a cooldown action-bar message.
    public void updateTargetOnly(Player tracker, Player target) {
        try {
            if (config.getBoolean(dev.quacc.playertrackerr.config.ConfigOption.DEBUG)) {
                JavaPlugin.getPlugin(PlayerTrackerr.class).getLogger().info("[PT Debug] updateTargetOnly for " + tracker.getName() + " -> " + target.getName());
            }
            updateHeldCompass(tracker, target.getLocation());
        } catch (Throwable ignored) {}
    }

    private void updateHeldCompass(Player tracker, Location targetLocation) {
        try {
            // Only update the player's global compass target when they are holding the configured compass
            // and it matches the tracked item id (if provided)
            boolean holding = false;
            var main = tracker.getInventory().getItemInMainHand();
            if (main != null && config.isConfiguredCompass(main)) {
                if (trackedItemId == null) holding = true;
                else {
                    var meta = main.getItemMeta();
                    if (meta != null && meta.getPersistentDataContainer().has(new NamespacedKey(JavaPlugin.getPlugin(PlayerTrackerr.class), "pt_unique"), PersistentDataType.STRING)) {
                        var id = meta.getPersistentDataContainer().get(new NamespacedKey(JavaPlugin.getPlugin(PlayerTrackerr.class), "pt_unique"), PersistentDataType.STRING);
                        if (trackedItemId.equals(id)) holding = true;
                    }
                }
            }

            var off = tracker.getInventory().getItemInOffHand();
            if (!holding && off != null && config.isConfiguredCompass(off)) {
                if (trackedItemId == null) holding = true;
                else {
                    var meta = off.getItemMeta();
                    if (meta != null && meta.getPersistentDataContainer().has(new NamespacedKey(JavaPlugin.getPlugin(PlayerTrackerr.class), "pt_unique"), PersistentDataType.STRING)) {
                        var id = meta.getPersistentDataContainer().get(new NamespacedKey(JavaPlugin.getPlugin(PlayerTrackerr.class), "pt_unique"), PersistentDataType.STRING);
                        if (trackedItemId.equals(id)) holding = true;
                    }
                }
            }

            if (holding) {
                if (config.getBoolean(dev.quacc.playertrackerr.config.ConfigOption.DEBUG)) {
                    JavaPlugin.getPlugin(PlayerTrackerr.class).getLogger().info("[PT Debug] Updating compass target for " + tracker.getName() + " (trackedId=" + trackedItemId + ")");
                }
                tracker.setCompassTarget(targetLocation);
            }
        } catch (Throwable ignored) {}
    }

    public void consumeDurability(Player tracker) {
        try {
            final var inv = tracker.getInventory();
            int slot = -1;
            org.bukkit.inventory.ItemStack item = null;

            if (trackedItemId != null) {
                try {
                    NamespacedKey uniqueKey = new NamespacedKey(JavaPlugin.getPlugin(PlayerTrackerr.class), "pt_unique");
                    for (int i = 0; i < inv.getSize(); i++) {
                        var it = inv.getItem(i);
                        if (it == null) continue;
                        var metaCheck = it.getItemMeta();
                        if (metaCheck == null) continue;
                        var pdcCheck = metaCheck.getPersistentDataContainer();
                        if (pdcCheck.has(uniqueKey, PersistentDataType.STRING)) {
                            var id = pdcCheck.get(uniqueKey, PersistentDataType.STRING);
                            if (trackedItemId.equals(id)) { item = it; slot = i; break; }
                        }
                    }
                } catch (Throwable ignored) {}
            } else {
                slot = inv.getHeldItemSlot();
                item = inv.getItem(slot);
            }

            if (item == null) {
                try { if (config.getBoolean(dev.quacc.playertrackerr.config.ConfigOption.DEBUG)) JavaPlugin.getPlugin(PlayerTrackerr.class).getLogger().info("[PT Debug] consumeDurability: no item found for tracker " + tracker.getName()); } catch (Throwable ignored) {}
                return;
            }
            if (!config.isConfiguredCompass(item)) {
                try { if (config.getBoolean(dev.quacc.playertrackerr.config.ConfigOption.DEBUG)) JavaPlugin.getPlugin(PlayerTrackerr.class).getLogger().info("[PT Debug] consumeDurability: item in slot " + slot + " is not recognised as configured compass for " + tracker.getName()); } catch (Throwable ignored) {}
                return;
            }

            // First, check for plugin-managed durability stored in PersistentDataContainer
            try {
                final org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
                JavaPlugin plugin = JavaPlugin.getPlugin(PlayerTrackerr.class);
                NamespacedKey maxKey = new NamespacedKey(plugin, "pt_max_durability");
                NamespacedKey curKey = new NamespacedKey(plugin, "pt_durability");
                var pdc = meta.getPersistentDataContainer();
                if (pdc.has(maxKey, PersistentDataType.INTEGER)) {
                    int max = pdc.get(maxKey, PersistentDataType.INTEGER);
                    int cur = pdc.has(curKey, PersistentDataType.INTEGER) ? pdc.get(curKey, PersistentDataType.INTEGER) : max;
                    long now = System.currentTimeMillis();
                    if (now - lastConsumedTime < 500L) return;
                    cur = Math.max(0, cur - 1);
                    lastConsumedTime = now;
                    try {
                        if (config.getBoolean(dev.quacc.playertrackerr.config.ConfigOption.DEBUG)) {
                            JavaPlugin.getPlugin(PlayerTrackerr.class).getLogger().info("[PT Debug] consumeDurability plugin-managed for " + tracker.getName() + ": remaining=" + cur + " / max=" + max);
                        }
                    } catch (Throwable ignored) {}

                    if (cur <= 0 && item.getAmount() <= 1) {
                        if (slot >= 0) {
                            inv.setItem(slot, null);
                            try { tracker.playSound(tracker.getLocation(), org.bukkit.Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f); } catch (Throwable ignored) {}
                        } else {
                            // try to remove from main or off hand if still held
                            var mainHand = inv.getItemInMainHand();
                            var offHand = inv.getItemInOffHand();
                            boolean removed = false;
                            try {
                                if (mainHand != null) {
                                    var m = mainHand.getItemMeta();
                                    if (m != null) {
                                        var uk = new NamespacedKey(JavaPlugin.getPlugin(PlayerTrackerr.class), "pt_unique");
                                        if (m.getPersistentDataContainer().has(uk, PersistentDataType.STRING)) {
                                            var id = m.getPersistentDataContainer().get(uk, PersistentDataType.STRING);
                                            if (trackedItemId != null && trackedItemId.equals(id)) {
                                                inv.setItemInMainHand(null);
                                                removed = true;
                                            }
                                        }
                                    }
                                }
                            } catch (Throwable ignored) {}
                            try {
                                if (!removed && offHand != null) {
                                    var m2 = offHand.getItemMeta();
                                    if (m2 != null) {
                                        var uk2 = new NamespacedKey(JavaPlugin.getPlugin(PlayerTrackerr.class), "pt_unique");
                                        if (m2.getPersistentDataContainer().has(uk2, PersistentDataType.STRING)) {
                                            var id2 = m2.getPersistentDataContainer().get(uk2, PersistentDataType.STRING);
                                            if (trackedItemId != null && trackedItemId.equals(id2)) {
                                                inv.setItemInOffHand(null);
                                                removed = true;
                                            }
                                        }
                                    }
                                }
                            } catch (Throwable ignored) {}
                            if (!removed) {
                                inv.removeItem(item);
                            }
                            try { tracker.playSound(tracker.getLocation(), org.bukkit.Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f); } catch (Throwable ignored) {}
                        }
                        try { if (config.getBoolean(dev.quacc.playertrackerr.config.ConfigOption.DEBUG)) JavaPlugin.getPlugin(PlayerTrackerr.class).getLogger().info("[PT Debug] Removed tracked item from inventory for " + tracker.getName()); } catch (Throwable ignored) {}
                        try { tracker.updateInventory(); } catch (Throwable ignored) {}
                    } else if (cur <= 0) {
                        item.setAmount(item.getAmount() - 1);
                        if (item.getAmount() > 0) {
                            pdc.set(curKey, PersistentDataType.INTEGER, 0);
                            // update lore to show reset durability
                            try {
                                java.util.List<String> loreList = meta.hasLore() ? new java.util.ArrayList<>(meta.getLore()) : new java.util.ArrayList<>();
                                loreList.removeIf(s -> s != null && s.toLowerCase().contains("durability"));
                                loreList.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7Durability: &a0&7/&f" + max));
                                meta.setLore(loreList);
                            } catch (Throwable ignored) {}
                            item.setItemMeta(meta);
                            if (slot >= 0) inv.setItem(slot, item);
                            try { tracker.updateInventory(); } catch (Throwable ignored) {}
                            try { if (config.getBoolean(dev.quacc.playertrackerr.config.ConfigOption.DEBUG)) JavaPlugin.getPlugin(PlayerTrackerr.class).getLogger().info("[PT Debug] Decremented tracked item stack for " + tracker.getName()); } catch (Throwable ignored) {}
                        }
                    } else {
                        pdc.set(curKey, PersistentDataType.INTEGER, cur);
                        // update lore with current durability
                        try {
                            java.util.List<String> loreList = meta.hasLore() ? new java.util.ArrayList<>(meta.getLore()) : new java.util.ArrayList<>();
                            loreList.removeIf(s -> s != null && s.toLowerCase().contains("durability"));
                            loreList.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7Durability: &a" + cur + "&7/&f" + max));
                            meta.setLore(loreList);
                        } catch (Throwable ignored) {}
                        item.setItemMeta(meta);
                        if (slot >= 0) inv.setItem(slot, item);
                        try { tracker.updateInventory(); } catch (Throwable ignored) {}
                        try { if (config.getBoolean(dev.quacc.playertrackerr.config.ConfigOption.DEBUG)) JavaPlugin.getPlugin(PlayerTrackerr.class).getLogger().info("[PT Debug] Updated tracked item durability to " + cur + " for " + tracker.getName()); } catch (Throwable ignored) {}
                    }
                    return;
                }
                    else {
                        try { if (config.getBoolean(dev.quacc.playertrackerr.config.ConfigOption.DEBUG)) JavaPlugin.getPlugin(PlayerTrackerr.class).getLogger().info("[PT Debug] consumeDurability: plugin-managed keys not present on item in slot " + slot + " for " + tracker.getName()); } catch (Throwable ignored) {}
                    }
            } catch (Throwable ignored) {}

            // Fallback: use Damageable meta when available (vanilla tools)
            final org.bukkit.inventory.meta.ItemMeta meta2 = item.getItemMeta();
            if (!(meta2 instanceof org.bukkit.inventory.meta.Damageable)) {
                try { if (config.getBoolean(dev.quacc.playertrackerr.config.ConfigOption.DEBUG)) JavaPlugin.getPlugin(PlayerTrackerr.class).getLogger().info("[PT Debug] consumeDurability: item meta not Damageable (vanilla durability unavailable) for " + tracker.getName() + " on slot " + slot); } catch (Throwable ignored) {}
                return;
            }
            final org.bukkit.inventory.meta.Damageable dmg = (org.bukkit.inventory.meta.Damageable) meta2;
            final int current = dmg.getDamage();
            int max = item.getType().getMaxDurability();
            final int next = current + 1;
            if (next >= max && item.getAmount() <= 1) {
                if (slot >= 0) inv.setItem(slot, null);
                else {
                    // attempt main/off hand removal
                    var mainHand = inv.getItemInMainHand();
                    var offHand = inv.getItemInOffHand();
                    boolean removed = false;
                    try {
                        if (mainHand != null && mainHand.isSimilar(item)) { inv.setItemInMainHand(null); removed = true; }
                    } catch (Throwable ignored) {}
                    try {
                        if (!removed && offHand != null && offHand.isSimilar(item)) { inv.setItemInOffHand(null); removed = true; }
                    } catch (Throwable ignored) {}
                    if (!removed) inv.removeItem(item);
                }
                try { if (config.getBoolean(dev.quacc.playertrackerr.config.ConfigOption.DEBUG)) JavaPlugin.getPlugin(PlayerTrackerr.class).getLogger().info("[PT Debug] Removed item due to vanilla durability for " + tracker.getName()); } catch (Throwable ignored) {}
                try { tracker.playSound(tracker.getLocation(), org.bukkit.Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f); } catch (Throwable ignored) {}
                try { tracker.updateInventory(); } catch (Throwable ignored) {}
            } else if (next >= max) {
                item.setAmount(item.getAmount() - 1);
                if (item.getAmount() > 0) {
                    dmg.setDamage(0);
                    item.setItemMeta(dmg);
                    if (slot >= 0) inv.setItem(slot, item);
                    try { tracker.updateInventory(); } catch (Throwable ignored) {}
                    try { if (config.getBoolean(dev.quacc.playertrackerr.config.ConfigOption.DEBUG)) JavaPlugin.getPlugin(PlayerTrackerr.class).getLogger().info("[PT Debug] Decremented vanilla item stack for " + tracker.getName()); } catch (Throwable ignored) {}
                }
            } else {
                dmg.setDamage(next);
                item.setItemMeta(dmg);
                if (slot >= 0) inv.setItem(slot, item);
                try { tracker.updateInventory(); } catch (Throwable ignored) {}
                try { if (config.getBoolean(dev.quacc.playertrackerr.config.ConfigOption.DEBUG)) JavaPlugin.getPlugin(PlayerTrackerr.class).getLogger().info("[PT Debug] Set vanilla damage to " + next + " for " + tracker.getName()); } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
    }

        private void sendHUD(Player tracker, Player target, int secondsUntilUpdate) {
            // Only send the action bar when the compass actually updated (secondsUntilUpdate == 0)
            // or when the remaining seconds value changed (once per second). This prevents
            // flooding the action bar while the player simply holds the compass.
            if (secondsUntilUpdate != 0 && secondsUntilUpdate == lastSentRemaining) return;

            final int distance = this.cachedDistance >= 0 ? this.cachedDistance : (int) tracker.getLocation().distance(target.getLocation());

            final String targetName = config.getBoolean(ConfigOption.TRACKING_SHOW_TARGET) ? target.getName() : "Ukryty";
            final Map<String, String> vars = Map.of(
                "target", targetName,
                "distance", String.valueOf(distance),
                "time", String.valueOf(secondsUntilUpdate)
            );

            tracker.spigot().sendMessage(
                ChatMessageType.ACTION_BAR,
                new ComponentBuilder(
                    config.format(ConfigOption.TRACKING_MESSAGE, vars)
                ).create()
            );

                // Optionally consume durability when the HUD remaining seconds decreases
                if (config.getBoolean(dev.quacc.playertrackerr.config.ConfigOption.COMPASS_CONSUME_ON_HUD_REFRESH)) {
                    if (secondsUntilUpdate != 0 && lastSentRemaining > 0 && secondsUntilUpdate < lastSentRemaining) {
                        try { if (config.getBoolean(dev.quacc.playertrackerr.config.ConfigOption.DEBUG)) JavaPlugin.getPlugin(PlayerTrackerr.class).getLogger().info("[PT Debug] sendHUD: HUD refresh will call consumeDurability for " + tracker.getName() + " (sec=" + secondsUntilUpdate + ", last=" + lastSentRemaining + ")"); } catch (Throwable ignored) {}
                        consumeDurability(tracker);
                    }
                }

                lastSentRemaining = secondsUntilUpdate;
        }

}
