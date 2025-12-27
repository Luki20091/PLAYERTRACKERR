package dev.quacc.playertrackerr.command.impl;

import dev.quacc.playertrackerr.PlayerTrackerr;
import dev.quacc.playertrackerr.command.CommandParameters;
import dev.quacc.playertrackerr.command.PlayerTrackerCommand;
import dev.quacc.playertrackerr.config.ConfigOption;
import dev.quacc.playertrackerr.config.ConfigOptionsManager;
import net.md_5.bungee.api.chat.*;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

@CommandParameters(
            name = "playertracker",
            aliases = {"pt"},
            usage = "/playertracker",
            requiredArgs = 0,
            permissions = {},
            playerOnly = true,
            description = "Information command for PlayerTracker"
)

public class PlayerTrackerInfoCommand extends PlayerTrackerCommand {

    public PlayerTrackerInfoCommand(PlayerTrackerr plugin, ConfigOptionsManager configManager) {
        super(plugin, configManager);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("pt.reload")) {
                sender.sendMessage(getConfigManager().format(ConfigOption.NO_PERMISSION));
                return;
            }

            getConfigManager().reload();
            sender.sendMessage(getConfigManager().format(ConfigOption.RELOADED_CONFIG_MESSAGE));
            return;
        }

        final Player player = (Player) sender;
        player.spigot().sendMessage(getInfoHelp());
    }

    private TextComponent getInfoHelp() {
        final var cfg = getConfigManager();
        final var entries = getPlugin().getCommandRegistration().getCommandHelp();

        final TextComponent root = new TextComponent("");

        root.addExtra(cfg.colorize("\n&a&lPLAYER TRACKER - &aVersion: " + getPlugin().getDescription().getVersion() + "\n"));
        root.addExtra(cfg.colorize("&7&m--------------------------------------------\n"));

        for (var e : entries) {
            final String usage = e.usage();
            final String desc  = e.description();

            final TextComponent line = new TextComponent(
                    cfg.colorize(" &a» &2" + usage + " &7- &f" + desc)
            );

            line.setClickEvent(new ClickEvent(
                    ClickEvent.Action.SUGGEST_COMMAND,
                    usage
            ));

            line.setHoverEvent(new HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    new Text(cfg.colorize("&aClick to insert\n&f" + usage))
            ));

            root.addExtra(line);
            root.addExtra("\n");
        }

        final String reloadCmd = "/playertracker reload";
        final TextComponent reloadLine = new TextComponent(
                cfg.colorize(" &a» &2" + reloadCmd + " &7- &fReload configuration\n")
        );

        reloadLine.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, reloadCmd));
        reloadLine.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new Text(cfg.colorize("&aClick to insert\n&f" + reloadCmd))));

        root.addExtra(reloadLine);
        root.addExtra(cfg.colorize("&7&m--------------------------------------------\n"));

        return root;
    }

}
