package dev.quacc.playertrackerr.tracking.helper;

import dev.quacc.playertrackerr.PlayerTrackerr;
import dev.quacc.playertrackerr.config.ConfigOptionsManager;
import dev.quacc.playertrackerr.tracking.TrackingManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AdminMenuHelper {

    private final PlayerTrackerr plugin;
    private final TrackingManager trackingManager;
    private final ConfigOptionsManager config;
    private final Set<Player> viewers = ConcurrentHashMap.newKeySet();

    public AdminMenuHelper(PlayerTrackerr plugin, ConfigOptionsManager config) {
        this.plugin = plugin;
        this.trackingManager = plugin.getTrackingManager();
        this.config = config;
    }

    public void open(Player viewer) {
        viewers.add(viewer);
        Inventory inventory = createInventory();
        viewer.openInventory(inventory);
    }

    public void close(Player viewer) {
        viewers.remove(viewer);
        viewer.closeInventory();
    }

    public void refreshAll() {
        final Inventory updated = createInventory();

        for (Player viewer : viewers) {
            if (!viewer.isOnline()) continue;

            final var open = viewer.getOpenInventory();
            final String title = ChatColor.stripColor(open.getTitle());

            if (!title.equalsIgnoreCase("Active Tracking Sessions")) continue;

            final Inventory current = open.getTopInventory();
            if (current.getSize() != updated.getSize()) continue; // avoid crafting grid mismatch

            current.setContents(updated.getContents());
            Bukkit.getScheduler().runTaskLater(plugin, viewer::updateInventory, 1L);
        }
    }

    private Inventory createInventory() {
        Inventory inventory = Bukkit.createInventory(null, 54, config.colorize("&2&lActive Tracking Sessions"));

        trackingManager.getTrackTask().getSessions().values().forEach(session -> {
            final Player tracker = Bukkit.getPlayer(session.getTrackerId());
            final Player target = Bukkit.getPlayer(session.getTargetId());
            if (tracker == null || target == null) return;

            final ItemStack item = new ItemStack(Material.COMPASS);
            final ItemMeta meta = item.getItemMeta();
            if (meta == null) return;

            meta.setDisplayName(config.colorize("&e" + tracker.getName() + " &7→ &c" + target.getName()));
            meta.setLore(config.colorizeList(Arrays.asList(
                    "&8&m---------------------",
                    "&7Tracker: &e" + tracker.getName(),
                    "&7Target: &c" + target.getName(),
                    "&8&m---------------------",
                    "&aClick to stop tracking"
            )));

            item.setItemMeta(meta);
            inventory.addItem(item);
        });

        return inventory;
    }
}