package dev.quacc.playertrackerr;

import dev.quacc.playertrackerr.config.ConfigOptionsManager;
import dev.quacc.playertrackerr.registration.CommandRegistration;
import dev.quacc.playertrackerr.tracking.helper.AdminMenuHelper;
import dev.quacc.playertrackerr.tracking.listener.AdminMenuClickListener;
import dev.quacc.playertrackerr.tracking.listener.CompassClickListener;
import dev.quacc.playertrackerr.tracking.TrackTask;
import dev.quacc.playertrackerr.tracking.TrackingManager;
import net.milkbowl.vault.economy.Economy;
import dev.quacc.playertrackerr.tracking.listener.TrackingListener;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class PlayerTrackerr extends JavaPlugin {

    private TrackingManager trackingManager;
    private CommandRegistration commandRegistration;
    private AdminMenuHelper adminMenuHelper;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        final ConfigOptionsManager configManager = new ConfigOptionsManager(this);
        // Register plugin-managed compass recipe/item
        dev.quacc.playertrackerr.items.CustomCompassFactory.registerRecipe(this);
        final Economy economy = setupEconomy();
        final TrackTask trackTask = new TrackTask(this, configManager);

        this.trackingManager = new TrackingManager(trackTask, configManager, economy);
        this.adminMenuHelper = new AdminMenuHelper(this, configManager);
        this.commandRegistration = new CommandRegistration(this, configManager);

        commandRegistration.registerCommands();
        getServer().getPluginManager().registerEvents(new CompassClickListener(trackingManager, configManager), this);
        getServer().getPluginManager().registerEvents(new dev.quacc.playertrackerr.items.CompassCraftListener(configManager), this);
        getServer().getPluginManager().registerEvents(new dev.quacc.playertrackerr.items.AutoAssignListener(configManager), this);
        getServer().getPluginManager().registerEvents(new dev.quacc.playertrackerr.tracking.listener.TrackedItemListener(trackingManager), this);
        getServer().getPluginManager().registerEvents(new TrackingListener(trackingManager), this);
        getServer().getPluginManager().registerEvents(new AdminMenuClickListener(trackingManager, adminMenuHelper), this);
    }

    public TrackingManager getTrackingManager() {
        return this.trackingManager;
    }
    public CommandRegistration getCommandRegistration() {
        return this.commandRegistration;
    }
    public AdminMenuHelper getAdminMenuHelper() { return this.adminMenuHelper; }

    private Economy setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return null;
        final RegisteredServiceProvider<Economy> provider = getServer().getServicesManager().getRegistration(Economy.class);
        if (provider == null) return null;

        return provider.getProvider();
    }

    @Override
    public void onDisable() {
        if (trackingManager != null) {
            trackingManager.stopAll();
            getLogger().info("[PlayerTracker] PlayerTracker Disabled. All tracking stopped.");
        }

    }
}
