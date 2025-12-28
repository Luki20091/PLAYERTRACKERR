package dev.quacc.playertrackerr.tracking.listener;

import dev.quacc.playertrackerr.config.ConfigOption;
import dev.quacc.playertrackerr.config.ConfigOptionsManager;
import dev.quacc.playertrackerr.tracking.StopReason;
import dev.quacc.playertrackerr.tracking.TrackingManager;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Comparator;
import java.util.Map;

public class CompassClickListener implements Listener {

    private final TrackingManager trackingManager;
    private final ConfigOptionsManager config;

    public CompassClickListener(TrackingManager trackingManager, ConfigOptionsManager config) {
        this.trackingManager = trackingManager;
        this.config = config;
    }

    @EventHandler
    public void onCompassClick(PlayerInteractEvent event) {
        if (!config.getBoolean(ConfigOption.TRACK_NEAREST)) return;
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        if (!isCompass(event)) return;
        if (!isClick(event)) return;

        final Player tracker = event.getPlayer();
        if (isLeftClick(event)) {
            handleLeftClick(tracker);
            return;
        }

        // Require permission to start tracking via compass right-click
        if (!tracker.hasPermission("pt.compass")) {
            tracker.sendMessage(config.format(ConfigOption.NO_PERMISSION));
            return;
        }

        if (!canUse(tracker)) return;
        handleRightClick(tracker);
    }

    private boolean canUse(Player tracker) {
        final long cooldownMs = config.getInt(ConfigOption.COOLDOWN_SECONDS) * 1000L;

        if (trackingManager.getCooldownHelper().isOnCooldown(tracker.getUniqueId(), cooldownMs)) {
            final long remaining = trackingManager.getCooldownHelper().getRemaining(tracker.getUniqueId(), cooldownMs);
            final String cooldownMessage = config.format(ConfigOption.COOLDOWN_MESSAGE,
                    Map.of("time", String.valueOf(remaining)));

            trackingManager.getTrackTask().suppressHUD(tracker.getUniqueId(), 1000);
            tracker.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    new ComponentBuilder(cooldownMessage).create());
            return false;
        }

        trackingManager.getCooldownHelper().apply(tracker.getUniqueId());
        return true;
    }

    private boolean isCompass(PlayerInteractEvent event) {
        final org.bukkit.inventory.ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
        return config.isConfiguredCompass(item);
    }

    private boolean isClick(PlayerInteractEvent event) {
        return switch (event.getAction()) {
            case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK, LEFT_CLICK_AIR, LEFT_CLICK_BLOCK -> true;
            default -> false;
        };
    }

    private boolean isLeftClick(PlayerInteractEvent event) {
        return event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK;
    }

    private void handleLeftClick(Player tracker) {
        if (!trackingManager.isTracking(tracker)) return;
        trackingManager.stopTracking(tracker, StopReason.SELF_STOP);
    }

    private void handleRightClick(Player tracker) {
        final Player nearest = nearestPlayer(tracker);
        if (nearest == null) {
            tracker.sendMessage(config.format(ConfigOption.NO_CLOSE_PLAYER));
            return;
        }
        // Ensure the specific ItemStack has a unique id if stackable is disabled
        String trackedId = null;
        try {
            final org.bukkit.inventory.ItemStack item = tracker.getInventory().getItemInMainHand();
            if (item != null && config.isConfiguredCompass(item)) {
                var meta = item.getItemMeta();
                if (meta != null) {
                    var pdc = meta.getPersistentDataContainer();
                    var uniqueKey = new org.bukkit.NamespacedKey(org.bukkit.plugin.java.JavaPlugin.getPlugin(dev.quacc.playertrackerr.PlayerTrackerr.class), "pt_unique");
                    if (pdc.has(uniqueKey, org.bukkit.persistence.PersistentDataType.STRING)) {
                        trackedId = pdc.get(uniqueKey, org.bukkit.persistence.PersistentDataType.STRING);
                    } else {
                        // create unique id for this stack if stacking is disabled so we can track the specific item
                        if (!config.getBoolean(dev.quacc.playertrackerr.config.ConfigOption.COMPASS_STACKABLE)) {
                            trackedId = java.util.UUID.randomUUID().toString();
                            pdc.set(uniqueKey, org.bukkit.persistence.PersistentDataType.STRING, trackedId);
                            item.setItemMeta(meta);
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}

        trackingManager.startTracking(tracker, nearest, trackedId);

        // Immediately perform an update for this session so the right-click triggers
        // the compass update and consumes durability (if enabled) without risking
        // a double-consume from the scheduled task.
        try {
            var session = trackingManager.getTrackTask().getSessions().get(tracker.getUniqueId());
            if (session != null) session.updateCompassIfAllowed(tracker, nearest);
        } catch (Throwable ignored) {}
    }

    private Player nearestPlayer(Player tracker) {
        final int maxDist = config.getInt(ConfigOption.TRACKING_DISTANCE);
        final double maxDistSquared = maxDist * maxDist;
        final Location trackerLocation = tracker.getLocation();

        return Bukkit.getOnlinePlayers().stream()
                .filter(target -> target != tracker)
                // Skip players who have bypass permission so we search the next eligible player
                .filter(target -> !target.hasPermission("pt.bypass"))
                .filter(target -> target.getWorld().equals(tracker.getWorld()))
                .filter(target -> target.getLocation().distanceSquared(trackerLocation) <= maxDistSquared)
                .min(Comparator.comparingDouble(target -> target.getLocation().distanceSquared(trackerLocation)))
                .orElse(null);
    }

}
