package dev.quacc.playertrackerr.tracking.helper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownHelper {

    private final Map<UUID, Long> timestamps = new HashMap<>();

    public boolean isOnCooldown(UUID uuid, long cooldownMs) {
        final long now = System.currentTimeMillis();
        final long last = timestamps.getOrDefault(uuid, 0L);
        return (now - last) < cooldownMs;
    }

    public long getRemaining(UUID uuid, long cooldownMs) {
        final long now = System.currentTimeMillis();
        final long last = timestamps.getOrDefault(uuid, 0L);
        return Math.max(0, (cooldownMs - (now - last)) / 1000L);
    }

    public void apply(UUID uuid) {
        timestamps.put(uuid, System.currentTimeMillis());
    }
}