package pine.flexspawn.util;

// 用法：负责 Bukkit LocationData 与 YAML 结构之间的读写转换。
import pine.flexspawn.model.LocationData;
import java.util.Optional;
import org.bukkit.configuration.ConfigurationSection;

public final class LocationSerializer {

    private LocationSerializer() {
    }

    public static Optional<LocationData> readOptional(ConfigurationSection root, String path) {
        ConfigurationSection section = root.getConfigurationSection(path);
        if (section == null) {
            return Optional.empty();
        }
        return Optional.of(readSection(section));
    }

    public static LocationData readRequired(ConfigurationSection root, String path) {
        ConfigurationSection section = root.getConfigurationSection(path);
        if (section == null) {
            throw new IllegalStateException("缺少位置配置节点: " + path);
        }
        return readSection(section);
    }

    public static void write(ConfigurationSection root, String path, LocationData locationData) {
        root.set(path + ".world", locationData.world());
        root.set(path + ".x", locationData.x());
        root.set(path + ".y", locationData.y());
        root.set(path + ".z", locationData.z());
        root.set(path + ".yaw", locationData.yaw());
        root.set(path + ".pitch", locationData.pitch());
    }

    public static void clear(ConfigurationSection root, String path) {
        root.set(path, null);
    }

    private static LocationData readSection(ConfigurationSection section) {
        String world = requireString(section, "world");
        double x = requireNumber(section, "x");
        double y = requireNumber(section, "y");
        double z = requireNumber(section, "z");
        float yaw = (float) requireNumber(section, "yaw");
        float pitch = (float) requireNumber(section, "pitch");
        return new LocationData(world, x, y, z, yaw, pitch);
    }

    private static String requireString(ConfigurationSection section, String key) {
        String value = section.getString(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("缺少字符串配置: " + fullPath(section, key));
        }
        return value;
    }

    private static double requireNumber(ConfigurationSection section, String key) {
        Object value = section.get(key);
        if (value == null) {
            throw new IllegalStateException("缺少数值配置: " + fullPath(section, key));
        }
        if (!(value instanceof Number number)) {
            throw new IllegalStateException("配置必须是数值类型: " + fullPath(section, key));
        }
        return number.doubleValue();
    }

    private static String fullPath(ConfigurationSection section, String key) {
        return section.getCurrentPath() + "." + key;
    }
}
