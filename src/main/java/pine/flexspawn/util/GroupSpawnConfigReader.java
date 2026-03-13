package pine.flexspawn.util;

// 用法：读取 config.yml 与 groups.yml 中的组坐标引用和候选点配置。
import pine.flexspawn.model.GroupSpawnReference;
import pine.flexspawn.model.LocationData;
import pine.flexspawn.model.WeightedLocationEntry;
import org.bukkit.configuration.ConfigurationSection;

public final class GroupSpawnConfigReader {

    private GroupSpawnConfigReader() {
    }

    public static GroupSpawnReference readReference(ConfigurationSection root, String path) {
        ConfigurationSection section = root.getConfigurationSection(path);
        if (section == null) {
            throw new IllegalStateException("缺少坐标组引用配置节点: " + path);
        }

        String group = section.getString("group");
        if (group == null || group.isBlank()) {
            throw new IllegalStateException("缺少坐标组名称配置: " + path + ".group");
        }

        String point = section.getString("point");
        if (point != null && point.isBlank()) {
            point = null;
        }
        return new GroupSpawnReference(group, point);
    }

    public static WeightedLocationEntry readWeightedLocationEntry(
            ConfigurationSection permissionSection,
            String pointName
    ) {
        LocationData locationData = LocationSerializer.readRequired(permissionSection, pointName);
        int weight = readWeight(permissionSection, pointName);
        return new WeightedLocationEntry(permissionSection.getName(), pointName, weight, locationData);
    }

    public static boolean hasPoint(ConfigurationSection groupSection, String pointName) {
        for (String permissionGroup : groupSection.getKeys(false)) {
            ConfigurationSection permissionSection = findChildSectionIgnoreCase(groupSection, permissionGroup);
            if (permissionSection != null && findKeyIgnoreCase(permissionSection, pointName) != null) {
                return true;
            }
        }
        return false;
    }

    public static ConfigurationSection findChildSectionIgnoreCase(
            ConfigurationSection parentSection,
            String childName
    ) {
        String resolvedChildName = findKeyIgnoreCase(parentSection, childName);
        if (resolvedChildName == null) {
            return null;
        }
        return parentSection.getConfigurationSection(resolvedChildName);
    }

    public static String findKeyIgnoreCase(ConfigurationSection section, String targetKey) {
        for (String key : section.getKeys(false)) {
            if (key.equalsIgnoreCase(targetKey)) {
                return key;
            }
        }
        return null;
    }

    private static int readWeight(ConfigurationSection permissionSection, String pointName) {
        Object weightValue = permissionSection.get(pointName + ".weight");
        if (!(weightValue instanceof Number weightNumber)) {
            throw new IllegalStateException(
                    "缺少权重配置或权重类型错误: "
                            + permissionSection.getCurrentPath()
                            + "."
                            + pointName
                            + ".weight"
            );
        }

        int weight = weightNumber.intValue();
        if (weight <= 0) {
            throw new IllegalStateException(
                    "权重必须大于 0: "
                            + permissionSection.getCurrentPath()
                            + "."
                            + pointName
                            + ".weight"
            );
        }
        return weight;
    }
}
