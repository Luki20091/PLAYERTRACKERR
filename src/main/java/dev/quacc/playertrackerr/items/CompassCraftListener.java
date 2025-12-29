package dev.quacc.playertrackerr.items;

import dev.quacc.playertrackerr.PlayerTrackerr;
import dev.quacc.playertrackerr.config.ConfigOptionsManager;
import dev.quacc.playertrackerr.config.ConfigOption;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
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
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        try {
            var inv = event.getInventory();
            var result = inv.getResult();
            if (result == null) return;
            if (!config.isConfiguredCompass(result)) return;

            // Only clear the prepared result if NONE of the viewers have permission
            // to craft the plugin compass. This avoids blocking the result when the
            // current player does have the permission while another viewer does not.
            var viewers = inv.getViewers();
            if (viewers == null || viewers.isEmpty()) return;
            boolean anyAllowed = false;
            for (var v : viewers) {
                if (v instanceof org.bukkit.entity.Player pl) {
                    try { if (pl.hasPermission("pt.craftcompass")) { anyAllowed = true; break; } } catch (Throwable ignored) {}
                }
            }
            if (!anyAllowed) {
                inv.setResult(null);
                return;
            }

            // If crafting is allowed and we want non-stackable compasses,
            // ensure the prepared result contains a unique id so crafted
            // items don't stack.
            if (!config.getBoolean(ConfigOption.COMPASS_STACKABLE)) {
                try {
                    ItemStack copy = result.clone();
                    var meta = copy.getItemMeta();
                    if (meta != null) {
                        NamespacedKey uniqueKey = new NamespacedKey(JavaPlugin.getPlugin(PlayerTrackerr.class), "pt_unique");
                        meta.getPersistentDataContainer().set(uniqueKey, PersistentDataType.STRING, UUID.randomUUID().toString());
                        copy.setItemMeta(meta);
                        inv.setResult(copy);
                    }
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
    }
}
