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

    public TrackingSession(UUID trackerId, UUID targetId, ConfigOptionsManager config, String trackedItemId) {
        this.trackerId = trackerId;
        this.targetId = targetId;
        this.config = config;
        this.compassHelper = new CompassHelper(config);
        this.trackedItemId = trackedItemId;
    }

    public void setPaused(boolean value) {
        this.paused = value;
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
                tracker.setCompassTarget(targetLocation);
            }
        } catch (Throwable ignored) {}
    }

    public void consumeDurability(Player tracker) {
        try {
            final org.bukkit.inventory.ItemStack item = tracker.getInventory().getItemInMainHand();
            if (item == null) return;
            if (!config.isConfiguredCompass(item)) return;
            // If a specific trackedItemId is set, ensure we only consume durability for that exact item
            if (trackedItemId != null) {
                final var metaCheck = item.getItemMeta();
                if (metaCheck == null) return;
                var pdcCheck = metaCheck.getPersistentDataContainer();
                var uniqueKey = new NamespacedKey(JavaPlugin.getPlugin(PlayerTrackerr.class), "pt_unique");
                if (!pdcCheck.has(uniqueKey, PersistentDataType.STRING)) return;
                var id = pdcCheck.get(uniqueKey, PersistentDataType.STRING);
                if (!trackedItemId.equals(id)) return;
            }
            final org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
            if (!(meta instanceof org.bukkit.inventory.meta.Damageable)) return;
            // First, check for plugin-managed durability stored in PersistentDataContainer
            try {
                JavaPlugin plugin = JavaPlugin.getPlugin(PlayerTrackerr.class);
                NamespacedKey maxKey = new NamespacedKey(plugin, "pt_max_durability");
                NamespacedKey curKey = new NamespacedKey(plugin, "pt_durability");
                var pdc = meta.getPersistentDataContainer();
                if (pdc.has(maxKey, PersistentDataType.INTEGER)) {
                    int max = pdc.get(maxKey, PersistentDataType.INTEGER);
                    int cur = pdc.has(curKey, PersistentDataType.INTEGER) ? pdc.get(curKey, PersistentDataType.INTEGER) : 0;
                    cur = cur + 1;
                    if (cur >= max && item.getAmount() <= 1) {
                        tracker.getInventory().setItemInMainHand(null);
                    } else if (cur >= max) {
                        item.setAmount(item.getAmount() - 1);
                        if (item.getAmount() > 0) {
                            pdc.set(curKey, PersistentDataType.INTEGER, 0);
                            item.setItemMeta(meta);
                        }
                    } else {
                        pdc.set(curKey, PersistentDataType.INTEGER, cur);
                        item.setItemMeta(meta);
                    }
                    return;
                }
            } catch (Throwable ignored) {}

            // Fallback: use Damageable meta when available (vanilla tools)
            final org.bukkit.inventory.meta.Damageable dmg = (org.bukkit.inventory.meta.Damageable) meta;
            final int current = dmg.getDamage();
            int max = item.getType().getMaxDurability();
            final int next = current + 1;
            if (next >= max && item.getAmount() <= 1) {
                tracker.getInventory().setItemInMainHand(null);
            } else if (next >= max) {
                item.setAmount(item.getAmount() - 1);
                if (item.getAmount() > 0) {
                    dmg.setDamage(0);
                    item.setItemMeta(dmg);
                }
            } else {
                dmg.setDamage(next);
                item.setItemMeta(dmg);
            }
        } catch (Throwable ignored) {}
    }

        private void sendHUD(Player tracker, Player target, int secondsUntilUpdate) {
            // Only send the action bar when the compass actually updated (secondsUntilUpdate == 0)
            // or when the remaining seconds value changed (once per second). This prevents
            // flooding the action bar while the player simply holds the compass.
            if (secondsUntilUpdate != 0 && secondsUntilUpdate == lastSentRemaining) return;

            final int distance = this.cachedDistance >= 0 ? this.cachedDistance : (int) tracker.getLocation().distance(target.getLocation());

            final String targetName = config.getBoolean(ConfigOption.TRACKING_SHOW_TARGET) ? target.getName() : "Hidden";
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
                        consumeDurability(tracker);
                    }
                }

                lastSentRemaining = secondsUntilUpdate;
        }

}
