package dev.quacc.playertrackerr.tracking;

import dev.quacc.playertrackerr.PlayerTrackerr;
import dev.quacc.playertrackerr.config.ConfigOptionsManager;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
public class TrackTask {

    private final PlayerTrackerr plugin;
    private final ConfigOptionsManager config;
    // map of tracker UUID to their scheduled update task
    private final java.util.Map<java.util.UUID, BukkitRunnable> sessionTasks = new java.util.concurrent.ConcurrentHashMap<>();

    public TrackTask(PlayerTrackerr plugin, ConfigOptionsManager config) {
        this.plugin = plugin;
        this.config = config;

        // No global loop — schedule per-session tasks when tracking starts
    }

    /*
    Used for suppressing the tracking message while player is in a click cooldown for compass
     */
    private final Map<UUID, Long> suppressedHUD = new ConcurrentHashMap<>();

    public void suppressHUD(UUID tracker, long millis) {
        suppressedHUD.put(tracker, System.currentTimeMillis() + millis);
    }

    private boolean isHUDSuppressed(UUID tracker) {
        return suppressedHUD.getOrDefault(tracker, 0L) > System.currentTimeMillis();
    }

    /*
    Map of active <TRACKER, TARGET>
     */
    private final Map<UUID, TrackingSession> sessions = new ConcurrentHashMap<>();

    public Map<UUID, TrackingSession> getSessions() {
        return this.sessions;
    }

    public void startTracking(Player tracker, Player target, String trackedItemId) {
        final UUID trackerId = tracker.getUniqueId();
        final UUID targetId = target.getUniqueId();

        TrackingSession session = new TrackingSession(trackerId, targetId, config, trackedItemId);
        sessions.put(trackerId, session);
        plugin.getAdminMenuHelper().refreshAll();

        // Always schedule a per-session task every second (20 ticks).
        // The session itself decides when to perform an actual compass update;
        // running every second allows the action-bar countdown to refresh each second.
        long ticks = 20L;

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                final Player trackerPlayer = plugin.getServer().getPlayer(session.getTrackerId());
                final Player targetPlayer = plugin.getServer().getPlayer(session.getTargetId());

                if (trackerPlayer != null && targetPlayer != null && trackerPlayer.isOnline() && targetPlayer.isOnline()) {
                    // If HUD for this tracker is suppressed (e.g., they just attempted a search
                    // and we displayed a cooldown action-bar), we should avoid sending the
                    // session HUD which would overwrite the cooldown message. In that case
                    // only update the compass target without sending the HUD.
                    if (isHUDSuppressed(session.getTrackerId())) {
                        session.updateTargetOnly(trackerPlayer, targetPlayer);
                        return;
                    }

                    if (!session.validateAndUpdate(plugin, trackerPlayer, targetPlayer)) {
                        trackerPlayer.setCompassTarget(trackerPlayer.getWorld().getSpawnLocation());
                        // stop and cleanup
                        this.cancel();
                        sessions.remove(session.getTrackerId());
                        sessionTasks.remove(session.getTrackerId());
                        plugin.getAdminMenuHelper().refreshAll();
                    }
                } else {
                    // players missing or offline: stop and cleanup
                    this.cancel();
                    sessions.remove(session.getTrackerId());
                    sessionTasks.remove(session.getTrackerId());
                    plugin.getAdminMenuHelper().refreshAll();
                }
            }
        };

        sessionTasks.put(trackerId, task);
        task.runTaskTimer(plugin, 0L, ticks);
    }

    public void stopTracking(Player player) {
        final UUID uuid = player.getUniqueId();

        // cancel scheduled task if present
        final BukkitRunnable t = sessionTasks.remove(uuid);
        if (t != null) t.cancel();

        final boolean removed = sessions.entrySet().removeIf(entry ->
            entry.getKey().equals(uuid) ||
            entry.getValue().getTargetId().equals(uuid));

        if (removed) plugin.getAdminMenuHelper().refreshAll();
    }

    // Only called on server shutdown
    public void stopAllTracking() {
        sessions.clear();
        sessionTasks.values().forEach(BukkitRunnable::cancel);
        sessionTasks.clear();
    }

    public TrackingSession getSessionByTarget(Player target) {
        final UUID targetId = target.getUniqueId();
        return sessions.values().stream()
                .filter(session -> session.getTargetId().equals(targetId))
                .findFirst()
                .orElse(null);
    }

    public Player getTarget(Player tracker) {
        TrackingSession session = sessions.get(tracker.getUniqueId());
        if (session == null) return null;

        return tracker.getServer().getPlayer(session.getTargetId());
    }

    public boolean isTracking(Player tracker) {
        return sessions.containsKey(tracker.getUniqueId());
    }
    

}
