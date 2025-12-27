package dev.quacc.playertrackerr.tracking.listener;

import dev.quacc.playertrackerr.config.ConfigOption;
import dev.quacc.playertrackerr.config.ConfigOptionsManager;
import dev.quacc.playertrackerr.tracking.StopReason;
import dev.quacc.playertrackerr.tracking.TrackingManager;
import dev.quacc.playertrackerr.tracking.helper.CooldownHelper;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
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
    private final CooldownHelper cooldownHelper = new CooldownHelper();

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

        if (!canUse(tracker)) return;
        handleRightClick(tracker);
    }

    private boolean canUse(Player tracker) {
        final long cooldownMs = config.getInt(ConfigOption.COOLDOWN_SECONDS) * 1000L;

        if (cooldownHelper.isOnCooldown(tracker.getUniqueId(), cooldownMs)) {
            final long remaining = cooldownHelper.getRemaining(tracker.getUniqueId(), cooldownMs);
            final String cooldownMessage = config.format(ConfigOption.COOLDOWN_MESSAGE,
                    Map.of("time", String.valueOf(remaining)));

            trackingManager.getTrackTask().suppressHUD(tracker.getUniqueId(), 1000);
            tracker.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    new ComponentBuilder(cooldownMessage).create());
            return false;
        }

        cooldownHelper.apply(tracker.getUniqueId());
        return true;
    }

    private boolean isCompass(PlayerInteractEvent event) {
        return event.getPlayer().getInventory().getItemInMainHand().getType() == Material.COMPASS;
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
        trackingManager.startTracking(tracker, nearest);
    }

    private Player nearestPlayer(Player tracker) {
        final int maxDist = config.getInt(ConfigOption.TRACKING_DISTANCE);
        final double maxDistSquared = maxDist * maxDist;
        final Location trackerLocation = tracker.getLocation();

        return Bukkit.getOnlinePlayers().stream()
                .filter(target -> target != tracker)
                .filter(target -> target.getWorld().equals(tracker.getWorld()))
                .filter(target -> target.getLocation().distanceSquared(trackerLocation) <= maxDistSquared)
                .min(Comparator.comparingDouble(target -> target.getLocation().distanceSquared(trackerLocation)))
                .orElse(null);
    }

}
