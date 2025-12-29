package dev.quacc.playertrackerr.command.impl;

import dev.quacc.playertrackerr.PlayerTrackerr;
import dev.quacc.playertrackerr.command.CommandParameters;
import dev.quacc.playertrackerr.command.PlayerTrackerCommand;
import dev.quacc.playertrackerr.config.ConfigOptionsManager;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

@CommandParameters(name = "ptfixcompass", aliases = {"fixcompass"}, usage = "/ptfixcompass [player]", permissions = {"pt.fixcompass"}, playerOnly = false, description = "Assign pt_unique to configured compasses missing it")
public class FixCompassCommand extends PlayerTrackerCommand {

    public FixCompassCommand(PlayerTrackerr plugin, ConfigOptionsManager configManager) {
        super(plugin, configManager);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            // run for all online players
            int total = 0;
            for (Player p : getPlugin().getServer().getOnlinePlayers()) {
                total += fixPlayer(p);
            }
            sender.sendMessage("Assigned pt_unique to " + total + " compass(es) across online players.");
        } else {
            Player target = getPlugin().getServer().getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(getConfigManager().format(dev.quacc.playertrackerr.config.ConfigOption.PLAYER_NOT_ONLINE));
                return;
            }
            int count = fixPlayer(target);
            sender.sendMessage("Assigned pt_unique to " + count + " compass(es) for " + target.getName());
        }
    }

    private int fixPlayer(Player p) {
        int count = 0;
        try {
            NamespacedKey uniqueKey = new NamespacedKey(getPlugin(), "pt_unique");
            for (int i = 0; i < p.getInventory().getSize(); i++) {
                var item = p.getInventory().getItem(i);
                if (item == null) continue;
                if (!getConfigManager().isConfiguredCompass(item)) continue;
                var meta = item.getItemMeta();
                if (meta == null) continue;
                var pdc = meta.getPersistentDataContainer();
                if (!pdc.has(uniqueKey, PersistentDataType.STRING)) {
                    pdc.set(uniqueKey, PersistentDataType.STRING, java.util.UUID.randomUUID().toString());
                    item.setItemMeta(meta);
                    count++;
                }
            }
        } catch (Throwable ignored) {}
        return count;
    }
}
