package dev.quacc.playertrackerr.command.impl;

import dev.quacc.playertrackerr.PlayerTrackerr;
import dev.quacc.playertrackerr.command.CommandParameters;
import dev.quacc.playertrackerr.command.PlayerTrackerCommand;
import dev.quacc.playertrackerr.config.ConfigOptionsManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandParameters(
        name = "trackers",
        aliases = {},
        usage = "/trackers",
        requiredArgs = 0,
        permissions = {"pt.trackers"},
        playerOnly = true,
        description = "Interface of all tracking players"
)

public class PlayersTrackingCommand extends PlayerTrackerCommand {

    public PlayersTrackingCommand(PlayerTrackerr plugin, ConfigOptionsManager configManager) {
        super(plugin, configManager);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        getPlugin().getAdminMenuHelper().open((Player) sender);
    }
}
