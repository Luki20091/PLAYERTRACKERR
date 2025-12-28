package dev.quacc.playertrackerr.command.impl;

import dev.quacc.playertrackerr.PlayerTrackerr;
import dev.quacc.playertrackerr.command.CommandParameters;
import dev.quacc.playertrackerr.command.PlayerTrackerCommand;
import dev.quacc.playertrackerr.config.ConfigOptionsManager;
import dev.quacc.playertrackerr.items.CustomCompassFactory;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandParameters(name = "giveptcompass", aliases = {"givecompass"}, usage = "/giveptcompass [player]", permissions = {"pt.givecompass"}, playerOnly = false, description = "Give the configured PlayerTrackerr compass")
public class GiveCompassCommand extends PlayerTrackerCommand {

    public GiveCompassCommand(PlayerTrackerr plugin, ConfigOptionsManager configManager) {
        super(plugin, configManager);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player target = null;
        if (args.length == 0) {
            if (sender instanceof Player p) target = p;
            else {
                sender.sendMessage("Specify a player when running from console.");
                return;
            }
        } else {
            target = getPlugin().getServer().getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(getConfigManager().format(dev.quacc.playertrackerr.config.ConfigOption.PLAYER_NOT_ONLINE));
                return;
            }
        }

        var item = CustomCompassFactory.createCompass(getPlugin(), getConfigManager());
        int amount = getPlugin().getConfig().getInt("compass.recipe.result-amount", 1);
        if (!getConfigManager().getBoolean(dev.quacc.playertrackerr.config.ConfigOption.COMPASS_STACKABLE)) {
            item.setAmount(1);
        } else {
            item.setAmount(Math.max(1, amount));
        }
        target.getInventory().addItem(item);

        sender.sendMessage("Gave configured compass to " + target.getName());
        if (!sender.equals(target)) target.sendMessage("You have received a compass.");
    }
}
