package dev.quacc.playertrackerr.tracking.helper;

import dev.quacc.playertrackerr.config.ConfigOption;
import dev.quacc.playertrackerr.config.ConfigOptionsManager;
import org.bukkit.entity.Player;

import java.util.Arrays;

public class CompassHelper {

    public enum CompassState {
        UPDATE, PAUSE, STOP
    }

    private final ConfigOptionsManager config;

    public CompassHelper(ConfigOptionsManager config) {
        this.config = config;
    }

    public CompassState validate(Player tracker) {
        final boolean requireInHand = config.getBoolean(ConfigOption.REQUIRE_COMPASS_HAND);

        // Check inventory for at least one matching item (ItemsAdder id preferred)
        boolean hasItem = Arrays.stream(tracker.getInventory().getStorageContents())
            .anyMatch(item -> config.isConfiguredCompass(item));

        if (!hasItem) return CompassState.STOP;

        if (requireInHand && !config.isConfiguredCompass(tracker.getInventory().getItemInMainHand())) {
            return CompassState.PAUSE;
        }

        return CompassState.UPDATE;
    }

    
}