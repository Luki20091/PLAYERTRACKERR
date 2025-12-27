package dev.quacc.playertrackerr.tracking.helper;

import dev.quacc.playertrackerr.config.ConfigOption;
import dev.quacc.playertrackerr.config.ConfigOptionsManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Map;

public class FeeHelper {

    private final ConfigOptionsManager config;
    private final Economy economy;

    public FeeHelper(ConfigOptionsManager config, Economy economy) {
        this.config = config;
        this.economy = economy;
    }

    private boolean canAffordVault(Player tracker) {
        if (!config.getBoolean(ConfigOption.TRACKING_FEE_VAULT_ENABLED)) return true;
        if (economy == null) {
            Bukkit.getConsoleSender().sendMessage(
                    "[PlayerTracker] Economy fee is enabled, but Vault is missing or failed to load!"
            );
            return true;
        }

        final double amount = config.getInt(ConfigOption.TRACKING_FEE_VAULT_AMOUNT);
        return economy.has(tracker, amount);
    }

    private boolean canAffordItem(Player tracker) {
        if (!config.getBoolean(ConfigOption.TRACKING_FEE_ITEM_ENABLED)) return true;

        final Material material = config.getMaterial(ConfigOption.TRACKING_FEE_ITEM_MATERIAL);
        final int amount = config.getInt(ConfigOption.TRACKING_FEE_ITEM_AMOUNT);

        final int total = Arrays.stream(tracker.getInventory().getStorageContents())
                .filter(item -> item != null && item.getType() == material)
                .mapToInt(ItemStack::getAmount)
                .sum();

        return total >= amount;
    }

    private void sendInsufficientMessages(Player tracker, boolean vaultAffordable, boolean itemAffordable) {
        final boolean vaultRequired = config.getBoolean(ConfigOption.TRACKING_FEE_VAULT_ENABLED);
        final boolean itemRequired = config.getBoolean(ConfigOption.TRACKING_FEE_ITEM_ENABLED);

        if (vaultRequired && economy == null) {
            tracker.sendMessage(config.colorize("&cVault not loaded properly. Skipping economy fee"));
        }

        if (vaultRequired && economy != null && !vaultAffordable) {
            final double amount = config.getInt(ConfigOption.TRACKING_FEE_VAULT_AMOUNT);
            tracker.sendMessage(config.format(ConfigOption.INSUFFICIENT_FEE_VAULT, Map.of(
                    "amount", String.valueOf(amount)
            )));
        }

        if (itemRequired && !itemAffordable) {
            final Material material = config.getMaterial(ConfigOption.TRACKING_FEE_ITEM_MATERIAL);
            final int amount = config.getInt(ConfigOption.TRACKING_FEE_ITEM_AMOUNT);
            tracker.sendMessage(config.format(ConfigOption.INSUFFICIENT_FEE_ITEM, Map.of(
                    "amount", String.valueOf(amount),
                    "item", material.name()
            )));
        }

    }

    private void chargeVault(Player tracker) {
        if (economy == null) return;

        final double amount = config.getInt(ConfigOption.TRACKING_FEE_VAULT_AMOUNT);
        economy.withdrawPlayer(tracker, amount);

        tracker.sendMessage(config.format(ConfigOption.WITHDRAW_FEE_MESSAGE, Map.of(
                "amount", String.valueOf(amount)
        )));
    }

    private void chargeItem(Player tracker) {
        final Material material = config.getMaterial(ConfigOption.TRACKING_FEE_ITEM_MATERIAL);
        final int amount = config.getInt(ConfigOption.TRACKING_FEE_ITEM_AMOUNT);

        tracker.getInventory().removeItem(new ItemStack(material, amount));
        tracker.sendMessage(config.format(ConfigOption.ITEM_TAKEN_MESSAGE, Map.of(
                "amount", String.valueOf(amount),
                "item", material.name()
        )));
    }

    public boolean processFees(Player tracker) {
        final boolean vaultRequired = config.getBoolean(ConfigOption.TRACKING_FEE_VAULT_ENABLED);
        final boolean itemRequired = config.getBoolean(ConfigOption.TRACKING_FEE_ITEM_ENABLED);

        final boolean vaultAffordable = canAffordVault(tracker);
        final boolean itemAffordable = canAffordItem(tracker);

        if ((vaultRequired && !vaultAffordable)
        || (itemRequired && !itemAffordable)) {

            sendInsufficientMessages(tracker, vaultAffordable, itemAffordable);
            return false;
        }

        if (vaultRequired) chargeVault(tracker);
        if (itemRequired) chargeItem(tracker);

        return true;
    }

}
