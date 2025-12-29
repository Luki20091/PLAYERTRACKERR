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
        // Ensure plugin-managed PDC keys are set (in case the item originated elsewhere)
        try {
            var meta = item.getItemMeta();
            var plugin = getPlugin();
            var pdc = meta.getPersistentDataContainer();
            var maxKey = new org.bukkit.NamespacedKey(plugin, "pt_max_durability");
            var curKey = new org.bukkit.NamespacedKey(plugin, "pt_durability");
            var markerKey = new org.bukkit.NamespacedKey(plugin, "pt_custom_compass");
            if (!pdc.has(markerKey, org.bukkit.persistence.PersistentDataType.INTEGER)) {
                pdc.set(markerKey, org.bukkit.persistence.PersistentDataType.INTEGER, 1);
            }
            int max = getConfigManager().getInt(dev.quacc.playertrackerr.config.ConfigOption.COMPASS_MAX_DURABILITY);
            if (max > 0 && !pdc.has(maxKey, org.bukkit.persistence.PersistentDataType.INTEGER)) {
                pdc.set(maxKey, org.bukkit.persistence.PersistentDataType.INTEGER, max);
                // store remaining durability starting at max
                pdc.set(curKey, org.bukkit.persistence.PersistentDataType.INTEGER, max);
            }
            // If stacking disabled, ensure a unique id exists
            try {
                if (!getConfigManager().getBoolean(dev.quacc.playertrackerr.config.ConfigOption.COMPASS_STACKABLE)) {
                    var uniqueKey = new org.bukkit.NamespacedKey(plugin, "pt_unique");
                    if (!pdc.has(uniqueKey, org.bukkit.persistence.PersistentDataType.STRING)) {
                        pdc.set(uniqueKey, org.bukkit.persistence.PersistentDataType.STRING, java.util.UUID.randomUUID().toString());
                    }
                }
            } catch (Throwable ignored) {}

            item.setItemMeta(meta);
        } catch (Throwable ignored) {}

        target.getInventory().addItem(item);

        sender.sendMessage("Gave configured compass to " + target.getName());
        if (!sender.equals(target)) target.sendMessage("You have received a compass.");
    }
}
