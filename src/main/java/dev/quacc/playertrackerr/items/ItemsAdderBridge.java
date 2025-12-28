package dev.quacc.playertrackerr.items;

import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Reflection-based bridge for ItemsAdder to avoid compile-time dependency.
 */
public final class ItemsAdderBridge {

    private final boolean available;
    private final Method byItemStack;
    private final Method getNamespacedId;
    private final Method getId;
    private final Method getInstanceString;
    private final Method getInstanceStringInt;
    

    public ItemsAdderBridge() {
        Method byItem = null;
        Method nsId = null;
        Method id = null;
        Method instS = null;
        Method instSI = null;
            Method itemStack = null;
            Method cmd = null;
        boolean ok = false;
        try {
            Class<?> cls = Class.forName("dev.lone.itemsadder.api.CustomStack");

            byItem = cls.getMethod("byItemStack", ItemStack.class);

            nsId = findAnyMethod(cls, "getNamespacedID", "getNamespacedId", "getNamespacedIdentifier");
            id = findMethod(cls, "getId");

            instS = findStaticMethod(cls, "getInstance", String.class);
            instSI = findStaticMethod(cls, "getInstance", String.class, int.class);

            itemStack = findAnyMethod(cls, "getItemStack", "getItemStackValue", "asItemStack");

            cmd = findAnyMethod(cls, "getCustomModelData", "getCustomModelDataId", "getModelData", "getCustomModelDataValue");

            ok = (byItem != null) && (itemStack != null) && (instS != null || instSI != null);
        } catch (Throwable ignored) {
            ok = false;
        }

        this.available = ok;
        this.byItemStack = byItem;
        this.getNamespacedId = nsId;
        this.getId = id;
        this.getInstanceString = instS;
        this.getInstanceStringInt = instSI;
    }

    public boolean isAvailable() {
        return available;
    }

    public String getCustomId(ItemStack itemStackArg) {
        if (!available || itemStackArg == null) return null;
        try {
            Object customStack = byItemStack.invoke(null, itemStackArg);
            if (customStack == null) return null;
            if (getNamespacedId != null) {
                Object v = getNamespacedId.invoke(customStack);
                String s = toNonBlankString(v);
                if (s != null) return s;
            }
            if (getId != null) {
                Object v = getId.invoke(customStack);
                String s = toNonBlankString(v);
                if (s != null) return s;
            }
            return null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public int getMaxDurabilityForId(String idStr) {
        if (!available || idStr == null || idStr.isBlank()) return 0;
        try {
            Object customStack;
            if (getInstanceStringInt != null) {
                customStack = getInstanceStringInt.invoke(null, idStr, 1);
            } else {
                customStack = getInstanceString.invoke(null, idStr);
            }
            if (customStack == null) return 0;

            String[] candidates = new String[]{"getMaxDurability", "getDefaultMaxDurability", "getDurability", "getDefaultDurability", "getMaxDurabilityValue", "getDurabilityMax"};
            for (String name : candidates) {
                Method m = findAnyMethod(customStack.getClass(), name);
                if (m != null) {
                    try {
                        Object v = m.invoke(customStack);
                        int parsed = parseIntSafe(v);
                        if (parsed > 0) return parsed;
                    } catch (Throwable ignored) {
                    }
                }
            }

            Method cfgM = findAnyMethod(customStack.getClass(), "getConfig", "getValues", "getData");
            if (cfgM != null) {
                try {
                    Object cfg = cfgM.invoke(customStack);
                    if (cfg instanceof java.util.Map<?, ?> map) {
                        Object v = map.get("max_durability");
                        if (v == null) v = map.get("durability");
                        if (v == null) v = map.get("durablity");
                        int parsed = parseIntSafe(v);
                        if (parsed > 0) return parsed;
                    }
                } catch (Throwable ignored) {
                }
            }

            for (Field f : customStack.getClass().getDeclaredFields()) {
                try {
                    f.setAccessible(true);
                    Object v = f.get(customStack);
                    int parsed = parseIntSafe(v);
                    if (parsed > 0) return parsed;
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
            return 0;
        }
        return 0;
    }

    private static int parseIntSafe(Object v) {
        if (v == null) return 0;
        if (v instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(v).trim());
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private static Method findAnyMethod(Class<?> cls, String... names) {
        if (names == null) return null;
        for (String n : names) {
            if (n == null || n.isBlank()) continue;
            Method m = findMethod(cls, n);
            if (m != null) return m;
        }
        return null;
    }

    private static Method findMethod(Class<?> cls, String name) {
        try {
            return cls.getMethod(name);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private static Method findStaticMethod(Class<?> cls, String name, Class<?>... params) {
        try {
            return cls.getMethod(name, params);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private static String toNonBlankString(Object v) {
        if (v == null) return null;
        if (v instanceof String s) {
            if (s.isBlank()) return null;
            return s;
        }
        String s = String.valueOf(v).trim();
        if (s.isBlank()) return null;
        return s;
    }
}
