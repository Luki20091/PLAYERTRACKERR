package dev.quacc.playertrackerr.tracking;

import dev.quacc.playertrackerr.config.ConfigOption;
import dev.quacc.playertrackerr.config.ConfigOptionsManager;
import dev.quacc.playertrackerr.tracking.helper.CompassHelper;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;

public class TrackingSession {

    private final UUID trackerId;
    private final UUID targetId;
    private final ConfigOptionsManager config;
    private final CompassHelper compassHelper;
    private long lastCompassUpdate = 0L;
    // Last seconds-until-update value sent to the player's action bar — prevents spamming the action bar
    private int lastSentRemaining = -1;
    // Distance caching to respect a separate distance update cooldown
    private long lastDistanceUpdate = 0L;
    private int cachedDistance = -1;

    public TrackingSession(UUID trackerId, UUID targetId, ConfigOptionsManager config) {
        this.trackerId = trackerId;
        this.targetId = targetId;
        this.config = config;
        this.compassHelper = new CompassHelper(config);
    }

    public UUID getTrackerId() {
        return this.trackerId;
    }

    public UUID getTargetId() {
        return this.targetId;
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

                if (compassUpdated) tracker.setCompassTarget(target.getLocation());

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
            tracker.setCompassTarget(target.getLocation());
            // When we actually update the compass, send HUD immediately with 0 seconds remaining
            if (config.getBoolean(dev.quacc.playertrackerr.config.ConfigOption.COMPASS_CONSUME_DURABILITY)) {
                consumeDurability(tracker);
            }
            sendHUD(tracker, target, 0);
            lastSentRemaining = 0;
        }
    }

    private void consumeDurability(Player tracker) {
        try {
            final org.bukkit.inventory.ItemStack item = tracker.getInventory().getItemInMainHand();
            if (item == null) return;
            if (!config.isConfiguredCompass(item)) return;
            final org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
            if (!(meta instanceof org.bukkit.inventory.meta.Damageable)) return;

            final org.bukkit.inventory.meta.Damageable dmg = (org.bukkit.inventory.meta.Damageable) meta;
            final int current = dmg.getDamage();
            int max = item.getType().getMaxDurability();

            // Prefer ItemsAdder-configured max durability if available
            try {
                var ia = config.getItemsAdder();
                if (ia != null && ia.isAvailable()) {
                    String iaId = ia.getCustomId(item);
                    int iaMax = ia.getMaxDurabilityForId(iaId);
                    if (iaMax > 0) max = iaMax;
                }
            } catch (Throwable ignored) {}
            final int next = current + 1;
            if (next >= max && item.getAmount() <= 1) {
                tracker.getInventory().setItemInMainHand(null);
            } else if (next >= max) {
                // consume one and reset damage for remaining stack
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
