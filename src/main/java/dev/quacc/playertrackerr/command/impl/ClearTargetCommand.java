package dev.quacc.playertrackerr.command.impl;

import dev.quacc.playertrackerr.PlayerTrackerr;
import dev.quacc.playertrackerr.command.CommandParameters;
import dev.quacc.playertrackerr.command.PlayerTrackerCommand;
import dev.quacc.playertrackerr.config.ConfigOption;
import dev.quacc.playertrackerr.config.ConfigOptionsManager;
import dev.quacc.playertrackerr.tracking.StopReason;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;



@CommandParameters(
        name = "cleartarget",
        aliases = {"ct"},
        usage = "/cleartarget, /cleartarget (player)",
        requiredArgs = 0,
        permissions = {"pt.cleartarget"},
        playerOnly = true,
        description = "Clear your own tracking, or another players"
)

public class ClearTargetCommand extends PlayerTrackerCommand {
    public ClearTargetCommand(PlayerTrackerr plugin, ConfigOptionsManager configManager) {
        super(plugin, configManager);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        final Player clearer = (Player) sender;

        if (args.length == 1) clearOther(clearer, args[0]); else clearSelf(clearer);
    }

    private void clearSelf(Player clearer) {
        final var trackingManager = getPlugin().getTrackingManager();

        if (!trackingManager.isTracking(clearer)) {
            clearer.sendMessage(getConfigManager().colorize("&cYou are currently not tracking anyone!"));
            return;
        }

        trackingManager.stopTracking(clearer, StopReason.SELF_STOP);
    }

    private void clearOther(Player clearer, String name) {
        final var trackingManager = getPlugin().getTrackingManager();

        if (!clearer.hasPermission("pt.cleartarget.other")) {
            clearer.sendMessage(getConfigManager().format(ConfigOption.NO_PERMISSION));
            return;
        }

        final Player tracker = Bukkit.getPlayerExact(name);

        if (tracker == null || !tracker.isOnline()) {
            clearer.sendMessage(getConfigManager().format(ConfigOption.PLAYER_NOT_ONLINE));
            return;
        }

        if (!trackingManager.isTracking(tracker)) {
            clearer.sendMessage(getConfigManager().colorize("&f" + tracker.getName() + " &cis not currently tracking anyone!"));
            return;
        }

        trackingManager.stopTracking(tracker, StopReason.FORCED_STOP);
        clearer.sendMessage(getConfigManager().colorize("&cStopped tracking for &f" + tracker.getName()));
    }

}
