package dev.quacc.playertrackerr.tracking;

import dev.quacc.playertrackerr.config.ConfigOption;
import dev.quacc.playertrackerr.config.ConfigOptionsManager;

import java.util.Map;

public enum StopReason {

    NO_COMPASS(ConfigOption.TRACKING_STOPPED_NO_COMPASS),
    OUT_OF_RANGE(ConfigOption.OUTSIDE_TRACKING_DISTANCE),
    WORLD_CHANGE(ConfigOption.CHANGED_WORLD),
    PLAYER_NOT_ONLINE(ConfigOption.PLAYER_NOT_ONLINE),
    TARGET_QUIT(ConfigOption.TARGET_LEFT),
    FORCED_STOP(ConfigOption.FORCED_STOP),
    SELF_STOP(ConfigOption.SELF_STOP);

    private final ConfigOption messageKey;

    StopReason(ConfigOption messageKey) {
        this.messageKey = messageKey;
    }

    public String format(ConfigOptionsManager config, String targetName) {
        return config.format(
                messageKey,
                targetName != null
                        ? Map.of("target", targetName)
                        : Map.of()
        );
    }

}
