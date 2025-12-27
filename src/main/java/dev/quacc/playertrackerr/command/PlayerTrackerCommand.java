package dev.quacc.playertrackerr.command;

import dev.quacc.playertrackerr.PlayerTrackerr;
import dev.quacc.playertrackerr.config.ConfigOption;
import dev.quacc.playertrackerr.config.ConfigOptionsManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

public abstract class PlayerTrackerCommand implements CommandExecutor {

    private final PlayerTrackerr plugin;
    private final ConfigOptionsManager configManager;

    public PlayerTrackerCommand(PlayerTrackerr plugin, ConfigOptionsManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        CommandParameters info = this.getClass().getAnnotation(CommandParameters.class);

        if (info.playerOnly() && (!(sender instanceof Player))) {
            sender.sendMessage("[PLAYER TRACKER] THIS COMMAND CAN ONLY BE EXECUTED BY A PLAYER");
            return true;
        }

        if (args.length < info.requiredArgs()) {
            sender.sendMessage("Incorrect usage, correct: " + info.usage());
            return true;
        }

        if (info.permissions().length > 0 &&
                Arrays.stream(info.permissions()).noneMatch(sender::hasPermission)) {
            sender.sendMessage(getConfigManager().format(ConfigOption.NO_PERMISSION));
            return true;
        }
        execute(sender, args);
        return true;
    }

    protected abstract void execute(CommandSender sender, String[] args);

    protected PlayerTrackerr getPlugin() {
        return plugin;
    }

    protected ConfigOptionsManager getConfigManager() {
        return configManager;
    }

}
