package dev.quacc.playertrackerr.items;

import dev.quacc.playertrackerr.PlayerTrackerr;
import dev.quacc.playertrackerr.config.ConfigOptionsManager;
import dev.quacc.playertrackerr.config.ConfigOption;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public class CompassCraftListener implements Listener {

    private final ConfigOptionsManager config;

    public CompassCraftListener(ConfigOptionsManager config) {
        this.config = config;
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        try {
            var human = event.getWhoClicked();
            if (human instanceof org.bukkit.entity.Player p) {
                if (!p.hasPermission("pt.craftcompass")) {
                    p.sendMessage(config.format(dev.quacc.playertrackerr.config.ConfigOption.NO_PERMISSION));
                    event.setCancelled(true);
                    return;
                }
            }
            if (config.getBoolean(ConfigOption.COMPASS_STACKABLE)) return;
            ItemStack result = event.getInventory().getResult();
            if (result == null) return;
            if (!config.isConfiguredCompass(result)) return;

            ItemStack copy = result.clone();
            var meta = copy.getItemMeta();
            if (meta != null) {
                NamespacedKey uniqueKey = new NamespacedKey(JavaPlugin.getPlugin(PlayerTrackerr.class), "pt_unique");
                meta.getPersistentDataContainer().set(uniqueKey, PersistentDataType.STRING, UUID.randomUUID().toString());
                copy.setItemMeta(meta);
                event.getInventory().setResult(copy);
            }
        } catch (Throwable ignored) {}
    }
}
