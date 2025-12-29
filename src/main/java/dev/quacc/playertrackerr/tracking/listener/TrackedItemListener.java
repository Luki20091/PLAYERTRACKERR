package dev.quacc.playertrackerr.tracking.listener;

import dev.quacc.playertrackerr.tracking.TrackingManager;
import dev.quacc.playertrackerr.tracking.TrackTask;
import dev.quacc.playertrackerr.tracking.TrackingSession;
import dev.quacc.playertrackerr.tracking.StopReason;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Material;

 

public class TrackedItemListener implements Listener {

    private final TrackingManager trackingManager;

    public TrackedItemListener(TrackingManager trackingManager) {
        this.trackingManager = trackingManager;
    }

    private boolean playerIsHoldingTrackedId(Player player, String id) {
        try {
            NamespacedKey uniqueKey = new NamespacedKey(JavaPlugin.getPlugin(dev.quacc.playertrackerr.PlayerTrackerr.class), "pt_unique");
            var main = player.getInventory().getItemInMainHand();
            if (main != null && main.hasItemMeta()) {
                var pdc = main.getItemMeta().getPersistentDataContainer();
                if (pdc.has(uniqueKey, PersistentDataType.STRING) && id.equals(pdc.get(uniqueKey, PersistentDataType.STRING))) return true;
            }
            var off = player.getInventory().getItemInOffHand();
            if (off != null && off.hasItemMeta()) {
                var pdc = off.getItemMeta().getPersistentDataContainer();
                if (pdc.has(uniqueKey, PersistentDataType.STRING) && id.equals(pdc.get(uniqueKey, PersistentDataType.STRING))) return true;
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private boolean configIsHoldingConfiguredCompass(Player player) {
        try {
            var main = player.getInventory().getItemInMainHand();
            var off = player.getInventory().getItemInOffHand();
            NamespacedKey markerKey = new NamespacedKey(JavaPlugin.getPlugin(dev.quacc.playertrackerr.PlayerTrackerr.class), "pt_custom_compass");

            if (main != null && main.hasItemMeta()) {
                var meta = main.getItemMeta();
                if (meta.getPersistentDataContainer().has(markerKey, PersistentDataType.INTEGER)) return true;
                if (main.getType() == Material.COMPASS) return true;
            }

            if (off != null && off.hasItemMeta()) {
                var meta = off.getItemMeta();
                if (meta.getPersistentDataContainer().has(markerKey, PersistentDataType.INTEGER)) return true;
                if (off.getType() == Material.COMPASS) return true;
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private boolean sessionItemStillExists(Player player, TrackingSession session) {
        if (session == null) return false;
        final String id = session.getTrackedItemId();
        if (id == null) return true; // no specific item tracked -> ignore

        // scan player's inventory for an item with matching pt_unique
        try {
            var inv = player.getInventory();
            NamespacedKey uniqueKey = new NamespacedKey(JavaPlugin.getPlugin(dev.quacc.playertrackerr.PlayerTrackerr.class), "pt_unique");
            return java.util.Arrays.stream(inv.getContents())
                    .filter(i -> i != null && i.hasItemMeta())
                    .map(i -> i.getItemMeta().getPersistentDataContainer())
                    .anyMatch(pdc -> pdc.has(uniqueKey, PersistentDataType.STRING) && id.equals(pdc.get(uniqueKey, PersistentDataType.STRING)));
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void checkAndStopIfMissing(Player player) {
        if (!trackingManager.isTracking(player)) return;
        TrackTask task = trackingManager.getTrackTask();
        var sessions = task.getSessions();
        TrackingSession session = sessions.get(player.getUniqueId());
        if (session == null) return;

        boolean exists = sessionItemStillExists(player, session);
        if (!exists) {
            try {
                var plugin = JavaPlugin.getPlugin(dev.quacc.playertrackerr.PlayerTrackerr.class);
                if (plugin.getConfig().getBoolean("debug", false)) plugin.getLogger().info("[PT Debug] Tracked item missing for " + player.getName() + " - stopping session");
            } catch (Throwable ignored) {}
            trackingManager.stopTracking(player, StopReason.SELF_STOP);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player p)) return;
        if (!trackingManager.isTracking(p)) return;
        TrackTask task = trackingManager.getTrackTask();
        var session = task.getSessions().get(p.getUniqueId());
        if (session == null) return;

        // If the tracked item no longer exists, stop immediately
        if (!sessionItemStillExists(p, session)) {
            trackingManager.stopTracking(p, StopReason.SELF_STOP);
            return;
        }

        // Determine whether the player is currently holding the tracked item.
        // Inventory changes happen after this event resolves, so check shortly
        // after the click to see the final state.
        try {
            var plugin = JavaPlugin.getPlugin(dev.quacc.playertrackerr.PlayerTrackerr.class);
            new org.bukkit.scheduler.BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        // If the tracked item no longer exists, stop immediately
                        if (!sessionItemStillExists(p, session)) {
                            trackingManager.stopTracking(p, StopReason.SELF_STOP);
                            return;
                        }

                        // Check if player is holding the tracked item in main or off hand
                        boolean holding = false;
                        String id = session.getTrackedItemId();
                        if (id == null) {
                            holding = configIsHoldingConfiguredCompass(p); // no specific id -> treat holding by material/name
                        } else {
                            holding = playerIsHoldingTrackedId(p, id);
                        }

                        boolean paused = !holding;
                        session.setPaused(paused);
                        try {
                            var plugin = JavaPlugin.getPlugin(dev.quacc.playertrackerr.PlayerTrackerr.class);
                            if (plugin.getConfig().getBoolean("debug", false)) plugin.getLogger().info("[PT Debug] Session for " + p.getName() + " setPaused=" + paused + " (holding=" + holding + ")");
                        } catch (Throwable ignored) {}
                    } catch (Throwable ignored) {}
                }
            }.runTaskLater(plugin, 2L);
        } catch (Throwable ignored) {}
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        checkAndStopIfMissing(event.getPlayer());
    }

    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent event) {
        checkAndStopIfMissing(event.getPlayer());
    }

    @EventHandler
    public void onBreak(PlayerItemBreakEvent event) {
        checkAndStopIfMissing(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (trackingManager.isTracking(event.getPlayer())) {
            trackingManager.stopTracking(event.getPlayer(), StopReason.SELF_STOP);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        var p = event.getEntity();
        if (trackingManager.isTracking(p)) trackingManager.stopTracking(p, StopReason.SELF_STOP);
    }
}
