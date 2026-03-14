package pine.flexspawn.service;

// 用法：统一校验 groups.yml 的结构与 config.yml 中启用的坐标引用，避免写入后再热重载才失败。
import pine.flexspawn.model.WeightedLocationEntry;
import pine.flexspawn.util.GroupSpawnConfigReader;
import org.bukkit.Server;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public final class GroupConfigValidator {

    private GroupConfigValidator() {
    }

    public static void validate(
            YamlConfiguration groupsConfig,
            ConfigurationSection config,
            Server server
    ) {
        validateGroupsFile(groupsConfig, server);
        validateReferences(config, groupsConfig);
    }

    private static void validateGroupsFile(YamlConfiguration groupsConfig, Server server) {
        if (groupsConfig.getKeys(false).isEmpty()) {
            throw new IllegalStateException("groups.yml 中至少需要定义一个坐标组。");
        }

        for (String groupName : groupsConfig.getKeys(false)) {
            ConfigurationSection groupSection = GroupSpawnConfigReader.findChildSectionIgnoreCase(groupsConfig, groupName);
            if (groupSection == null) {
                throw new IllegalStateException("未找到坐标组配置: " + groupName);
            }
            if (groupSection.getKeys(false).isEmpty()) {
                throw new IllegalStateException("坐标组不能为空: " + groupName);
            }

            for (String permissionGroup : groupSection.getKeys(false)) {
                ConfigurationSection permissionSection = groupSection.getConfigurationSection(permissionGroup);
                if (permissionSection == null) {
                    throw new IllegalStateException("权限组配置格式错误: " + groupName + "." + permissionGroup);
                }
                if (permissionSection.getKeys(false).isEmpty()) {
                    throw new IllegalStateException("权限组内至少需要一个坐标点: " + groupName + "." + permissionGroup);
                }

                for (String pointName : permissionSection.getKeys(false)) {
                    WeightedLocationEntry entry = GroupSpawnConfigReader.readWeightedLocationEntry(
                            permissionSection,
                            pointName
                    );
                    entry.locationData().toLocation(server);
                }
            }
        }
    }

    private static void validateReferences(ConfigurationSection config, YamlConfiguration groupsConfig) {
        validateReferencesRecursively(config, groupsConfig);
    }

    private static void validateReferencesRecursively(
            ConfigurationSection currentSection,
            YamlConfiguration groupsConfig
    ) {
        if (isDisabled(currentSection)) {
            return;
        }

        validateCurrentReference(currentSection, groupsConfig);
        for (String key : currentSection.getKeys(false)) {
            ConfigurationSection childSection = currentSection.getConfigurationSection(key);
            if (childSection != null) {
                validateReferencesRecursively(childSection, groupsConfig);
            }
        }
    }

    private static boolean isDisabled(ConfigurationSection section) {
        return section.contains("enabled") && !section.getBoolean("enabled", true);
    }

    private static void validateCurrentReference(
            ConfigurationSection currentSection,
            YamlConfiguration groupsConfig
    ) {
        if (!currentSection.contains("group")) {
            return;
        }

        String groupName = currentSection.getString("group");
        if (groupName == null || groupName.isBlank()) {
            throw new IllegalStateException("缺少坐标组名称配置: " + currentSection.getCurrentPath() + ".group");
        }

        ConfigurationSection groupSection = GroupSpawnConfigReader.findChildSectionIgnoreCase(groupsConfig, groupName);
        if (groupSection == null) {
            throw new IllegalStateException(
                    "配置节点引用了不存在的坐标组: " + currentSection.getCurrentPath() + " -> " + groupName
            );
        }

        String pointName = currentSection.getString("point");
        if (pointName == null || pointName.isBlank()) {
            return;
        }
        if (!GroupSpawnConfigReader.hasPoint(groupSection, pointName)) {
            throw new IllegalStateException(
                    "坐标组中不存在指定坐标点: "
                            + currentSection.getCurrentPath()
                            + " -> "
                            + groupName
                            + "."
                            + pointName
            );
        }
    }
}
