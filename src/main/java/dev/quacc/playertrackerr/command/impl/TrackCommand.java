package dev.quacc.playertrackerr.command.impl;

import dev.quacc.playertrackerr.PlayerTrackerr;
import dev.quacc.playertrackerr.command.CommandParameters;
import dev.quacc.playertrackerr.command.PlayerTrackerCommand;
import dev.quacc.playertrackerr.config.ConfigOption;
import dev.quacc.playertrackerr.config.ConfigOptionsManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Map;
 

@CommandParameters(
            name = "track",
            aliases = {},
            usage = "/track (player)",
            requiredArgs = 1,
        permissions = {"pt.command.track"},
            playerOnly = true,
            description = "Track a player"
    )

    public class TrackCommand extends PlayerTrackerCommand {

        public TrackCommand(PlayerTrackerr plugin, ConfigOptionsManager configManager) {
            super(plugin, configManager);
        }

    @Override
    public void execute(CommandSender sender, String[] args) {
        final Player tracker = (Player) sender;

        final boolean hasCompass = Arrays.stream(tracker.getInventory().getStorageContents())
            .anyMatch(item -> getConfigManager().isConfiguredCompass(item));

        if (!hasCompass) {
            tracker.sendMessage(getConfigManager().format(ConfigOption.NO_COMPASS));
            return;
        }

        final Player target = findTarget(args[0]);

        if (target == null) {
            tracker.sendMessage(getConfigManager().format(ConfigOption.PLAYER_NOT_ONLINE, Map.of("target", args[0])));
            return;
        }

        getPlugin().getTrackingManager().startTracking(tracker, target);
    }

    private Player findTarget(String name) {
            return Bukkit.getOnlinePlayers().stream()
                    .filter(player -> player.getName().equalsIgnoreCase(name))
                    .findFirst()
                    .orElse(null);
    }
}
