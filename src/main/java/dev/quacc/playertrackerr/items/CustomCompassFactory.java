package dev.quacc.playertrackerr.items;

import dev.quacc.playertrackerr.config.ConfigOptionsManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import org.bukkit.enchantments.Enchantment;

@SuppressWarnings("deprecation")
public final class CustomCompassFactory {

    public static ItemStack createCompass(JavaPlugin plugin, ConfigOptionsManager config) {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        String name = plugin.getConfig().getString("compass.name", "&fStary Kompas: &aPiratów");
        meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', name));
        try {
            List<String> lore = plugin.getConfig().getStringList("compass.lore");
            if (lore != null && !lore.isEmpty()) {
                meta.setLore(lore.stream().map(s -> org.bukkit.ChatColor.translateAlternateColorCodes('&', s)).toList());
            }
        } catch (Throwable ignored) {}

        // plugin-managed durability via PersistentDataContainer
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey maxKey = new NamespacedKey(plugin, "pt_max_durability");
        NamespacedKey curKey = new NamespacedKey(plugin, "pt_durability");
        NamespacedKey markerKey = new NamespacedKey(plugin, "pt_custom_compass");
        int max = plugin.getConfig().getInt("compass.max-durability", 0);
        if (max > 0) {
            pdc.set(maxKey, PersistentDataType.INTEGER, max);
            pdc.set(curKey, PersistentDataType.INTEGER, 0);
        }
        // mark this item as the plugin's custom compass
        pdc.set(markerKey, PersistentDataType.INTEGER, 1);

        // If stackable is disabled, add a unique marker so items do not stack
        try {
            if (!config.getBoolean(dev.quacc.playertrackerr.config.ConfigOption.COMPASS_STACKABLE)) {
                NamespacedKey uniqueKey = new NamespacedKey(plugin, "pt_unique");
                pdc.set(uniqueKey, PersistentDataType.STRING, java.util.UUID.randomUUID().toString());
            }
        } catch (Throwable ignored) {}

        // apply optional enchantment from config
        try {
            if (config.getBoolean(dev.quacc.playertrackerr.config.ConfigOption.COMPASS_ENCHANT_ENABLED)) {
                String enchName = config.getString(dev.quacc.playertrackerr.config.ConfigOption.COMPASS_ENCHANT_TYPE);
                int lvl = config.getInt(dev.quacc.playertrackerr.config.ConfigOption.COMPASS_ENCHANT_LEVEL);
                if (enchName != null && !enchName.isBlank() && lvl > 0) {
                    // normalize to namespaced key (minecraft:vanishing_curse etc.)
                    String normalized = enchName.trim().toLowerCase().replace(' ', '_');
                    NamespacedKey enchKey = NamespacedKey.minecraft(normalized);
                    Enchantment ench = Enchantment.getByKey(enchKey);
                    if (ench != null) {
                        meta.addEnchant(ench, lvl, true);
                    } else {
                        plugin.getLogger().warning("Invalid enchant configured for compass: " + enchName);
                    }
                }
            }
        } catch (Throwable ignored) {}

        item.setItemMeta(meta);
        return item;
    }

    public static void registerRecipe(JavaPlugin plugin) {
        try {
            if (!plugin.getConfig().getBoolean("compass.recipe-enabled", true)) return;
            NamespacedKey key = new NamespacedKey(plugin, "playertracker_compass");
                ConfigOptionsManager mgr = new ConfigOptionsManager(plugin);
                ItemStack result = createCompass(plugin, mgr);
                // apply configured result amount (respect stackable config)
                int resultAmount = plugin.getConfig().getInt("compass.recipe.result-amount", 1);
                if (!mgr.getBoolean(dev.quacc.playertrackerr.config.ConfigOption.COMPASS_STACKABLE)) {
                    result.setAmount(1);
                } else {
                    result.setAmount(Math.max(1, resultAmount));
                }

            ShapedRecipe recipe = new ShapedRecipe(key, result);

            // read shape from config (3 strings)
            List<String> shape = plugin.getConfig().getStringList("compass.recipe.shape");
            if (shape == null || shape.size() < 3) {
                recipe.shape("XEX", "ECE", "XEX");
            } else {
                recipe.shape(shape.get(0), shape.get(1), shape.get(2));
            }

            // read ingredient mapping from config
            var section = plugin.getConfig().getConfigurationSection("compass.recipe.ingredients");
            if (section != null) {
                for (String keyChar : section.getKeys(false)) {
                    if (keyChar == null || keyChar.length() == 0) continue;
                    String matName = section.getString(keyChar, "");
                    if (matName == null || matName.isBlank()) continue;
                    try {
                        Material m = Material.valueOf(matName.toUpperCase());
                        recipe.setIngredient(keyChar.charAt(0), m);
                    } catch (IllegalArgumentException ignored) {
                        plugin.getLogger().warning("Invalid material in compass.recipe.ingredients: " + matName);
                    }
                }
            } else {
                // fallback to defaults
                recipe.setIngredient('E', Material.EMERALD);
                recipe.setIngredient('C', Material.COMPASS);
            }

            Bukkit.getServer().addRecipe(recipe);
        } catch (Throwable t) {
            plugin.getLogger().warning("Failed to register custom compass recipe: " + t.getMessage());
        }
    }
}
