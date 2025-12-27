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
    private BukkitRunnable updateTask;
    private final ConfigOptionsManager config;

    public TrackTask(PlayerTrackerr plugin, ConfigOptionsManager config) {
        this.plugin = plugin;
        this.config = config;

        this.startTask();
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

    public void startTracking(Player tracker, Player target) {
        final UUID trackerId = tracker.getUniqueId();
        final UUID targetId = target.getUniqueId();

        TrackingSession session = new TrackingSession(trackerId, targetId, config);
        sessions.put(trackerId, session);
        plugin.getAdminMenuHelper().refreshAll();
    }

    public void stopTracking(Player player) {
        final UUID uuid = player.getUniqueId();

        final boolean removed = sessions.entrySet().removeIf(entry ->
                entry.getKey().equals(uuid) ||
                entry.getValue().getTargetId().equals(uuid));

        if (removed) plugin.getAdminMenuHelper().refreshAll();
    }

    // Only called on server shutdown
    public void stopAllTracking() {
        sessions.clear();
        if (updateTask != null) updateTask.cancel();
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

    private void startTask() {
        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (sessions.isEmpty()) return;

                sessions.entrySet().removeIf(entry -> {
                    final TrackingSession session = entry.getValue();
                    final Player tracker = plugin.getServer().getPlayer(session.getTrackerId());
                    final Player target = plugin.getServer().getPlayer(session.getTargetId());

                    if (isHUDSuppressed(session.getTrackerId())) {
                        if (tracker != null && target != null && tracker.isOnline() && target.isOnline()) {
                            session.updateCompassIfAllowed(tracker, target);
                            return false;
                        }
                    }

                    if (!session.validateAndUpdate(plugin, tracker, target)) {
                        if (tracker != null && tracker.isOnline()) {
                            tracker.setCompassTarget(tracker.getWorld().getSpawnLocation());
                        }
                        return true;
                    }

                    return false;
                });
            }
        };

        updateTask.runTaskTimer(plugin, 0L, 4L);
    }

}
