package dev.quacc.playertrackerr.config;

import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.function.BiFunction;

public enum ConfigOption {

    TRACK_NEAREST("track-nearest", Type.BOOLEAN, true),
    TRACKING_DISTANCE("tracking-distance", Type.INTEGER, 10000),

    REQUIRE_COMPASS_HAND("compass.require-compass-hand", Type.BOOLEAN, true),
    COOLDOWN_SECONDS("compass.cooldown-seconds", Type.INTEGER, 5),
    COOLDOWN_MESSAGE("compass.search-cooldown", Type.STRING, "&cMusisz poczekać &f{time}s &czanim ponownie rozpoczniesz wyszukiwanie!"),
    COMPASS_UPDATE_COOLDOWN_ENABLED("compass.update-cooldown-enabled", Type.BOOLEAN, false),
    COMPASS_UPDATE_COOLDOWN_SECONDS("compass.update-cooldown-seconds", Type.INTEGER, 2),
    COMPASS_ITEM_MATERIAL("compass.item-material", Type.MATERIAL, Material.COMPASS),
    COMPASS_ITEM("compass.item", Type.STRING, "COMPASS"),
    COMPASS_CONSUME_DURABILITY("compass.consume-durability", Type.BOOLEAN, false),
    COMPASS_CONSUME_ON_HUD_REFRESH("compass.consume-on-hud-refresh", Type.BOOLEAN, false),
    COMPASS_RECIPE_RESULT_AMOUNT("compass.recipe.result-amount", Type.INTEGER, 1),
    COMPASS_STACKABLE("compass.stackable", Type.BOOLEAN, true),
    COMPASS_ENCHANT_ENABLED("compass.enchant.enabled", Type.BOOLEAN, false),
    COMPASS_ENCHANT_TYPE("compass.enchant.type", Type.STRING, "VANISHING_CURSE"),
    COMPASS_ENCHANT_LEVEL("compass.enchant.level", Type.INTEGER, 1),

    TRACKING_FEE_ITEM_ENABLED("tracking-fee-item.enabled", Type.BOOLEAN, true),
    TRACKING_FEE_ITEM_MATERIAL("tracking-fee-item.material", Type.MATERIAL, Material.DIAMOND),
    TRACKING_FEE_ITEM_AMOUNT("tracking-fee-item.amount", Type.INTEGER, 1),
    ITEM_TAKEN_MESSAGE("tracking-fee-item.item.taken.message", Type.STRING, "&cRequired tracking fee of &f{amount} {item} &ctaken"),

    TRACKING_FEE_VAULT_ENABLED("tracking-fee-vault.enabled", Type.BOOLEAN, false),
    TRACKING_FEE_VAULT_AMOUNT("tracking-fee-vault.amount", Type.INTEGER, 0),
    WITHDRAW_FEE_MESSAGE("tracking-fee-vault.withdraw.fee.message", Type.STRING, "&cWithdraw tracking fee of &f{amount} &cfrom your account!"),

    PLUGIN_PREFIX("plugin-prefix", Type.STRING, "&a&lTracker &8» "),
    NO_CLOSE_PLAYER("no-close-player", Type.STRING, "&cNo nearby players found to track!"),
    OUTSIDE_TRACKING_DISTANCE("outside-tracking-distance", Type.STRING, "&f{target} &chas moved too far away to track!"),
    INSUFFICIENT_FEE_ITEM("insufficient-fee-item", Type.STRING, "&cMissing required tracking fee: &f{amount} {item}"),
    INSUFFICIENT_FEE_VAULT("insufficient-fee-vault", Type.STRING, "&cYou need at least &f{amount}&c to track this player!"),
    NOTIFY_TARGET("notify-target", Type.STRING, "&cWARNING: You are being tracked by &f{tracker}"),
    TRACK_SELF("track-self", Type.STRING, "&cYou cannot track yourself!"),
    NO_PERMISSION("no-permission", Type.STRING, "&cYou do not have permission to do that."),
    RELOADED_CONFIG_MESSAGE("reloaded-config-message", Type.STRING, "&aConfiguration reloaded successfully!"),
    TRACKING_MESSAGE("tracking-message", Type.STRING, "&aTracking &e{target} &7| Distance: &b{distance}m"),
    TRACKING_SHOW_TARGET("tracking.show-target", Type.BOOLEAN, true),
    PLAYER_NOT_SPECIFIED("player-not-specified", Type.STRING, "&cPlease specify a player!"),
    PLAYER_NOT_ONLINE("player-not-online", Type.STRING, "&cThat player is not online!"),
    NOT_IN_WORLD("not-in-world", Type.STRING, "&f{target} &cis in another world — cannot track them!"),
    CHANGED_WORLD("changed-world", Type.STRING, "&cYour target changed worlds — tracking stopped!"),
    NO_COMPASS("no-compass", Type.STRING, "&cYou need a compass to use &f/track&c!"),
    TRACKING_STOPPED_NO_COMPASS("tracking-stopped-no-compass", Type.STRING, "&cCompass missing — tracking stopped!"),
    COMPASS_NOT_IN_HAND("compass-not-in-hand", Type.STRING, "&cYou must hold a compass to track!"),
    NOT_TRACKING_PLAYER("not-tracking-player", Type.STRING, "&cYou are not currently tracking anyone."),
    TRACKER_BYPASS_MESSAGE("tracker-bypass-message", Type.STRING, "&cThis player cannot be tracked."),
    FORCED_STOP("forced-stop", Type.STRING, "&cTracking of &f{target} &chas been forcefully stopped."),
    SELF_STOP("self-stop", Type.STRING, "&aYou have stopped tracking your target."),
    TARGET_LEFT("target-left", Type.STRING, "&cStopped tracking &f{target}&c — they left the server.");

    private final String path;
    private final Type type;
    private final Object defaultValue;

    ConfigOption(String path, Type type, Object defaultValue) {
        this.path = path;
        this.type = type;
        this.defaultValue = defaultValue;
    }

    public String getPath() { return this.path; }

    public Object getDefaultValue() { return this.defaultValue; }

    public <T> T get(JavaPlugin plugin, Class<T> clazz) {
        Object value = type.read(plugin, this);
        return clazz.isInstance(value) ? clazz.cast(value) : clazz.cast(defaultValue);
    }

    public enum Type {
        STRING((plugin, opt) -> plugin.getConfig().getString(opt.getPath(), (String) opt.getDefaultValue())),
        INTEGER((plugin, opt) -> plugin.getConfig().getInt(opt.getPath(), (Integer) opt.getDefaultValue())),
        BOOLEAN((plugin, opt) -> plugin.getConfig().getBoolean(opt.getPath(), (Boolean) opt.getDefaultValue())),
        MATERIAL((plugin, opt) -> {
            String name = plugin.getConfig().getString(opt.getPath(), opt.getDefaultValue().toString());
            try {
                return Material.valueOf(name.toUpperCase());
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("[PlayerTracker] Invalid material for " + opt.getPath() + "; using default.");
                return opt.getDefaultValue();
            }
        });

        private final BiFunction<JavaPlugin, ConfigOption, Object> reader;

        Type(BiFunction<JavaPlugin, ConfigOption, Object> reader) {
            this.reader = reader;
        }

        public Object read(JavaPlugin plugin, ConfigOption option) {
            return reader.apply(plugin, option);
        }
    }
}
