package dev.quacc.playertrackerr.items;

import dev.quacc.playertrackerr.config.ConfigOptionsManager;
import dev.quacc.playertrackerr.PlayerTrackerr;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class AutoAssignListener implements Listener {

    private final ConfigOptionsManager config;

    public AutoAssignListener(ConfigOptionsManager config) {
        this.config = config;
    }

    private void ensureUnique(ItemStack item) {
        if (item == null) return;
        if (!config.isConfiguredCompass(item)) return;
        try {
            if (!config.getBoolean(dev.quacc.playertrackerr.config.ConfigOption.COMPASS_STACKABLE)) {
                var meta = item.getItemMeta();
                if (meta == null) return;
                var pdc = meta.getPersistentDataContainer();
                NamespacedKey uniqueKey = new NamespacedKey(org.bukkit.plugin.java.JavaPlugin.getPlugin(PlayerTrackerr.class), "pt_unique");
                if (!pdc.has(uniqueKey, PersistentDataType.STRING)) {
                    pdc.set(uniqueKey, PersistentDataType.STRING, java.util.UUID.randomUUID().toString());
                    item.setItemMeta(meta);
                }
            }
        } catch (Throwable ignored) {}
    }

    @EventHandler
    public void onEntityPickup(EntityPickupItemEvent event) {
        try {
            var itemEntity = event.getItem();
            ItemStack stack = itemEntity.getItemStack();
            ensureUnique(stack);
            itemEntity.setItemStack(stack);
        } catch (Throwable ignored) {}
    }

    @EventHandler
    public void onInventoryMove(InventoryMoveItemEvent event) {
        try {
            ItemStack stack = event.getItem();
            ensureUnique(stack);
            event.setItem(stack);
        } catch (Throwable ignored) {}
    }

    @EventHandler
    public void onItemSpawn(ItemSpawnEvent event) {
        try {
            ItemStack stack = event.getEntity().getItemStack();
            ensureUnique(stack);
            event.getEntity().setItemStack(stack);
        } catch (Throwable ignored) {}
    }
}
