package dev.quacc.playertrackerr.config;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class ConfigOptionsManager {

    private final JavaPlugin plugin;
    private dev.quacc.playertrackerr.items.ItemsAdderBridge itemsAdder;

    public ConfigOptionsManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void setItemsAdder(dev.quacc.playertrackerr.items.ItemsAdderBridge bridge) {
        this.itemsAdder = bridge;
    }

    public dev.quacc.playertrackerr.items.ItemsAdderBridge getItemsAdder() {
        return this.itemsAdder;
    }

    public boolean isConfiguredCompass(org.bukkit.inventory.ItemStack item) {
        if (item == null) return false;
        // Prefer ItemsAdder id matching when available and configured
        try {
            final String configured = getString(ConfigOption.COMPASS_ITEM);
            if (configured != null && !configured.isBlank()) {
                // If configured looks like a namespaced ItemsAdder id (contains ':')
                var ia = this.itemsAdder;
                if (configured.contains(":")) {
                    if (ia != null && ia.isAvailable()) {
                        String id = ia.getCustomId(item);
                        return id != null && id.equalsIgnoreCase(configured);
                    }
                    // ItemsAdder not available -> cannot match ItemsAdder id
                    return false;
                }

                // Otherwise treat configured value as a Material name
                try {
                    org.bukkit.Material m = org.bukkit.Material.valueOf(configured.toUpperCase());
                    return item.getType() == m;
                } catch (IllegalArgumentException ignored) {
                    return false;
                }
            }
        } catch (Throwable ignored) {}

        // If no specific `compass.item` configured, fallback to legacy material config
        try {
            org.bukkit.Material legacy = ConfigOption.COMPASS_ITEM_MATERIAL.get(this.plugin, org.bukkit.Material.class);
            return item.getType() == legacy;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public String getString(ConfigOption option) {
        return option.get(plugin, String.class);
    }

    public boolean getBoolean(ConfigOption option) {
        return option.get(plugin, Boolean.class);
    }

    public int getInt(ConfigOption option) {
        return option.get(plugin, Integer.class);
    }

    public Material getMaterial(ConfigOption option) {
        return option.get(plugin, Material.class);
    }

    public String format(ConfigOption key) {
        return format(key, null);
    }

    public String format(ConfigOption option, Map<String, String> vars) {
        String message = getString(option);
        message = message.replace("{prefix}", getString(ConfigOption.PLUGIN_PREFIX));

        if (vars != null && !vars.isEmpty()) {
            for (Map.Entry<String, String> entry : vars.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }

        return colorize(message);
    }

    private final Pattern HEX_PATTERN = Pattern.compile("(?<!\\\\)(#[A-Fa-f0-9]{6})");

    public String colorize(String message) {
        if (message == null) return "";

        String result = message;
        final Matcher matcher = HEX_PATTERN.matcher(result);
        while (matcher.find()) {
            final String color = matcher.group(1);
            result = result.replace(color, ChatColor.of(color).toString());
        }
        return ChatColor.translateAlternateColorCodes('&', result);
    }

    public List<String> colorizeList(List<String> messages) {
        if (messages == null || messages.isEmpty()) return List.of();
        return messages.stream()
                .map(this::colorize)
                .collect(Collectors.toList());
    }

    public void reload() {
        plugin.reloadConfig();
        plugin.getConfig().options().copyDefaults(true);
        plugin.saveConfig();
    }

}
