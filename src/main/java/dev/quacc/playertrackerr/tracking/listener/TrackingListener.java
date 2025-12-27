package dev.quacc.playertrackerr.tracking.listener;

import dev.quacc.playertrackerr.tracking.StopReason;
import dev.quacc.playertrackerr.tracking.TrackingManager;
import dev.quacc.playertrackerr.tracking.TrackingSession;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class TrackingListener implements Listener {

    private final TrackingManager trackingManager;

    public TrackingListener(TrackingManager trackingManager) {
        this.trackingManager = trackingManager;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        final Player playerQuitting = event.getPlayer();

        if (trackingManager.isTracking(playerQuitting)) {
            trackingManager.stopTrackingSilent(playerQuitting);
            return;
        }

        final TrackingSession session = trackingManager.getTrackTask().getSessionByTarget(playerQuitting);
        if (session != null) {
            final Player tracker = playerQuitting.getServer().getPlayer(session.getTrackerId());
            if (tracker != null && tracker.isOnline())
                trackingManager.stopTracking(tracker, StopReason.TARGET_QUIT);
        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        final Player changer = event.getPlayer();

        if (trackingManager.isTracking(changer)) {
            trackingManager.stopTracking(changer, StopReason.WORLD_CHANGE);
            return;
        }

        final var session = trackingManager.getTrackTask().getSessionByTarget(changer);

        if (session != null) {
            final Player tracker = Bukkit.getPlayer(session.getTrackerId());
            trackingManager.stopTracking(tracker, StopReason.WORLD_CHANGE);
        }
    }

}
