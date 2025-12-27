package dev.quacc.playertrackerr.tracking;

import dev.quacc.playertrackerr.config.ConfigOption;
import dev.quacc.playertrackerr.config.ConfigOptionsManager;
import dev.quacc.playertrackerr.tracking.helper.FeeHelper;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;

import java.util.Map;

public class TrackingManager {

    private final TrackTask trackTask;
    private final ConfigOptionsManager config;
    private final FeeHelper feeHelper;

    public TrackingManager(TrackTask trackTask, ConfigOptionsManager config, Economy economy) {
        this.trackTask = trackTask;
        this.config = config;
        this.feeHelper = new FeeHelper(config, economy);
    }

    public void startTracking(Player tracker, Player target) {
        if (tracker.equals(target)) {
            tracker.sendMessage(config.format(ConfigOption.TRACK_SELF));
            return;
        }

        if (target.hasPermission("pt.bypass")) {
            tracker.sendMessage(config.format(ConfigOption.TRACKER_BYPASS_MESSAGE));
            return;
        }

        if (!target.getWorld().equals(tracker.getWorld())) {
            tracker.sendMessage(config.format(ConfigOption.NOT_IN_WORLD, Map.of("target", target.getName())));
            return;
        }

        if (!feeHelper.processFees(tracker)) return;
        if (trackTask.isTracking(tracker)) stopTrackingSilent(tracker);

        trackTask.startTracking(tracker, target);

        if (target.hasPermission("pt.notify"))
            target.sendMessage(config.format(ConfigOption.NOTIFY_TARGET, Map.of(
                    "tracker", tracker.getName()
            )));
    }

    public void stopTracking(Player tracker) {
        stopTracking(tracker, StopReason.FORCED_STOP);
    }

    public void stopTrackingSilent(Player tracker) {
        if (!trackTask.isTracking(tracker)) return;
        tracker.setCompassTarget(tracker.getWorld().getSpawnLocation());
        trackTask.stopTracking(tracker);
    }

    public void stopTracking(Player tracker, StopReason reason) {
        if (!trackTask.isTracking(tracker)) return;

        tracker.setCompassTarget(tracker.getWorld().getSpawnLocation());

        final Player target = getTarget(tracker);
        final String targetName = target != null ? target.getName() : null;

        trackTask.stopTracking(tracker);
        tracker.sendMessage(reason.format(config, targetName));
    }

    public Player getTarget(Player tracker) {
        return trackTask.getTarget(tracker);
    }

    public void stopAll() {
        trackTask.stopAllTracking();
    }

    public boolean isTracking(Player tracker) {
        return trackTask.isTracking(tracker);
    }

    public TrackTask getTrackTask() {
        return this.trackTask;
    }

}
