package me.mytheria.hoppers.hopper;

import me.mytheria.hoppers.MytheriaHoppers;
import org.bukkit.NamespacedKey;

public final class HopperKeys {

    private HopperKeys() {
    }

    public static NamespacedKey speedLevel() {
        return new NamespacedKey(
                MytheriaHoppers.getInstance(),
                "speed_level"
        );
    }

    public static NamespacedKey rangeLevel() {
        return new NamespacedKey(
                MytheriaHoppers.getInstance(),
                "range_level"
        );
    }

    public static NamespacedKey type() {
        return new NamespacedKey(MytheriaHoppers.getInstance(), "hopper_type");
    }
}
