package dev.quacc.playertrackerr.tracking.listener;

import dev.quacc.playertrackerr.tracking.StopReason;
import dev.quacc.playertrackerr.tracking.TrackingManager;
import dev.quacc.playertrackerr.tracking.helper.AdminMenuHelper;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class AdminMenuClickListener implements Listener {

    private final TrackingManager trackingManager;
    private final AdminMenuHelper menuManager;

    public AdminMenuClickListener(TrackingManager trackingManager, AdminMenuHelper menuHelper) {
        this.trackingManager = trackingManager;
        this.menuManager = menuHelper;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (event.getClickedInventory() == null) return;

        final String title = ChatColor.stripColor(event.getView().getTitle());
        if (!"Active Tracking Sessions".equalsIgnoreCase(title)) return;

        event.setCancelled(true);
        final ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() != Material.COMPASS) return;

        final ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        final String stripped = ChatColor.stripColor(meta.getDisplayName());
        final String[] parts = stripped.split("→");
        if (parts.length < 2) return;

        final Player tracker = Bukkit.getPlayerExact(parts[0].trim());

        if (tracker == null || !tracker.isOnline()) return;

        trackingManager.stopTracking(tracker, StopReason.FORCED_STOP);
        menuManager.refreshAll();
    }
}