package dev.quacc.playertrackerr.tracking.helper;

import dev.quacc.playertrackerr.config.ConfigOption;
import dev.quacc.playertrackerr.config.ConfigOptionsManager;
import org.bukkit.Material;
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

        if (Arrays.stream(tracker.getInventory().getStorageContents())
                .noneMatch(item -> item != null && item.getType() == Material.COMPASS)) {
            return CompassState.STOP;
        }

        if (requireInHand &&
                tracker.getInventory().getItemInMainHand().getType() != Material.COMPASS) {
            return CompassState.PAUSE;
        }

        return CompassState.UPDATE;
    }

}
