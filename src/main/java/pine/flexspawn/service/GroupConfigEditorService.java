package pine.flexspawn.service;

// 用法：负责 groups.yml 的查询、增删和传送定位，避免命令层直接操作 YAML。
import pine.flexspawn.model.LocationData;
import pine.flexspawn.model.WeightedLocationEntry;
import pine.flexspawn.util.GroupSpawnConfigReader;
import pine.flexspawn.util.LocationSerializer;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class GroupConfigEditorService {

    private final JavaPlugin plugin;
    private final PermissionSpawnSelector permissionSpawnSelector;
    private final WeightedLocationPicker weightedLocationPicker;
    private final Supplier<YamlConfiguration> runtimeGroupsConfigSupplier;

    public GroupConfigEditorService(
            JavaPlugin plugin,
            PermissionSpawnSelector permissionSpawnSelector,
            WeightedLocationPicker weightedLocationPicker
    ) {
        this(
                plugin,
                permissionSpawnSelector,
                weightedLocationPicker,
                () -> YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "groups.yml"))
        );
    }

    public GroupConfigEditorService(
            JavaPlugin plugin,
            PermissionSpawnSelector permissionSpawnSelector,
            WeightedLocationPicker weightedLocationPicker,
            Supplier<YamlConfiguration> runtimeGroupsConfigSupplier
    ) {
        this.plugin = plugin;
        this.permissionSpawnSelector = permissionSpawnSelector;
        this.weightedLocationPicker = weightedLocationPicker;
        this.runtimeGroupsConfigSupplier = runtimeGroupsConfigSupplier;
    }

    public synchronized List<String> getGroupNames() {
        return List.copyOf(loadRuntimeGroupsConfig().getKeys(false));
    }

    public synchronized List<String> getPermissionGroupNames(String groupName) {
        ConfigurationSection groupSection = resolveGroupSection(loadRuntimeGroupsConfig(), groupName);
        if (groupSection == null) {
            return List.of();
        }
        return List.copyOf(groupSection.getKeys(false));
    }

    public synchronized List<String> getCoordinateNames(String groupName) {
        ConfigurationSection groupSection = resolveGroupSection(loadRuntimeGroupsConfig(), groupName);
        if (groupSection == null) {
            return List.of();
        }

        Map<String, String> coordinateNames = new LinkedHashMap<>();
        for (String permissionGroup : groupSection.getKeys(false)) {
            ConfigurationSection permissionSection = groupSection.getConfigurationSection(permissionGroup);
            if (permissionSection == null) {
                continue;
            }
            for (String pointName : permissionSection.getKeys(false)) {
                coordinateNames.putIfAbsent(pointName.toLowerCase(Locale.ROOT), pointName);
            }
        }
        return List.copyOf(coordinateNames.values());
    }

    public synchronized boolean containsCoordinate(String groupName, String pointName) {
        ConfigurationSection groupSection = resolveGroupSection(loadRuntimeGroupsConfig(), groupName);
        return groupSection != null && GroupSpawnConfigReader.hasPoint(groupSection, pointName);
    }

    public synchronized boolean addCoordinate(
            String groupName,
            String permissionGroupName,
            String pointName,
            LocationData locationData,
            int weight
    ) {
        String validatedGroupName = requireName(groupName, "坐标组名称");
        String validatedPermissionGroupName = requireName(permissionGroupName, "权限组名称");
        String validatedPointName = requireName(pointName, "坐标点名称");
        if (weight <= 0) {
            throw new IllegalArgumentException("权重必须大于 0。");
        }

        YamlConfiguration groupsConfig = loadGroupsConfig();
        ConfigurationSection groupSection = resolveGroupSection(groupsConfig, validatedGroupName);
        if (groupSection == null) {
            throw new IllegalArgumentException("坐标组不存在: " + groupName);
        }

        ConfigurationSection permissionSection = resolvePermissionSection(groupSection, validatedPermissionGroupName);
        if (permissionSection == null) {
            throw new IllegalArgumentException("权限组不存在: " + permissionGroupName);
        }
        if (GroupSpawnConfigReader.findKeyIgnoreCase(permissionSection, validatedPointName) != null) {
            return false;
        }

        LocationSerializer.write(permissionSection, validatedPointName, locationData);
        permissionSection.set(validatedPointName + ".weight", weight);
        saveValidated(groupsConfig);
        return true;
    }

    public synchronized boolean deleteGroup(String groupName) {
        String validatedGroupName = requireName(groupName, "坐标组名称");
        YamlConfiguration groupsConfig = loadGroupsConfig();
        String resolvedGroupName = GroupSpawnConfigReader.findKeyIgnoreCase(groupsConfig, validatedGroupName);
        if (resolvedGroupName == null) {
            return false;
        }

        groupsConfig.set(resolvedGroupName, null);
        saveValidated(groupsConfig);
        return true;
    }

    public synchronized boolean deletePermissionGroup(String groupName, String permissionGroupName) {
        String validatedGroupName = requireName(groupName, "坐标组名称");
        String validatedPermissionGroupName = requireName(permissionGroupName, "权限组名称");
        YamlConfiguration groupsConfig = loadGroupsConfig();

        ConfigurationSection groupSection = resolveGroupSection(groupsConfig, validatedGroupName);
        if (groupSection == null) {
            return false;
        }

        String resolvedPermissionGroupName = GroupSpawnConfigReader.findKeyIgnoreCase(groupSection, validatedPermissionGroupName);
        if (resolvedPermissionGroupName == null) {
            return false;
        }
        if (groupSection.getKeys(false).size() == 1) {
            throw new IllegalStateException("坐标组至少需要保留一个权限组，请直接删除整个坐标组。");
        }

        groupSection.set(resolvedPermissionGroupName, null);
        saveValidated(groupsConfig);
        return true;
    }

    public synchronized boolean deleteCoordinate(String groupName, String permissionGroupName, String pointName) {
        String validatedGroupName = requireName(groupName, "坐标组名称");
        String validatedPermissionGroupName = requireName(permissionGroupName, "权限组名称");
        String validatedPointName = requireName(pointName, "坐标点名称");
        YamlConfiguration groupsConfig = loadGroupsConfig();

        ConfigurationSection groupSection = resolveGroupSection(groupsConfig, validatedGroupName);
        if (groupSection == null) {
            return false;
        }

        ConfigurationSection permissionSection = resolvePermissionSection(groupSection, validatedPermissionGroupName);
        if (permissionSection == null) {
            return false;
        }

        String resolvedPointName = GroupSpawnConfigReader.findKeyIgnoreCase(permissionSection, validatedPointName);
        if (resolvedPointName == null) {
            return false;
        }
        if (permissionSection.getKeys(false).size() == 1) {
            throw new IllegalStateException("权限组至少需要保留一个坐标点，请直接删除整个权限组。");
        }

        permissionSection.set(resolvedPointName, null);
        saveValidated(groupsConfig);
        return true;
    }

    public synchronized Optional<LocationData> findGroupLocation(String groupName, Player player) {
        ConfigurationSection groupSection = resolveGroupSection(loadRuntimeGroupsConfig(), groupName);
        if (groupSection == null) {
            return Optional.empty();
        }

        List<WeightedLocationEntry> candidates = permissionSpawnSelector.collectCandidates(player, groupSection, null);
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(weightedLocationPicker.pick(candidates).locationData());
    }

    public synchronized Optional<LocationData> findCoordinateLocation(String groupName, String pointName) {
        ConfigurationSection groupSection = resolveGroupSection(loadRuntimeGroupsConfig(), groupName);
        if (groupSection == null) {
            return Optional.empty();
        }

        LocationData matchedLocation = null;
        for (String permissionGroup : groupSection.getKeys(false)) {
            ConfigurationSection permissionSection = groupSection.getConfigurationSection(permissionGroup);
            if (permissionSection == null) {
                continue;
            }

            String resolvedPointName = GroupSpawnConfigReader.findKeyIgnoreCase(permissionSection, pointName);
            if (resolvedPointName == null) {
                continue;
            }

            LocationData locationData = LocationSerializer.readRequired(permissionSection, resolvedPointName);
            if (matchedLocation != null) {
                throw new IllegalStateException("坐标点名称不唯一，请改用唯一名称后再传送。");
            }
            matchedLocation = locationData;
        }
        return Optional.ofNullable(matchedLocation);
    }

    private ConfigurationSection resolveGroupSection(YamlConfiguration groupsConfig, String groupName) {
        return GroupSpawnConfigReader.findChildSectionIgnoreCase(groupsConfig, groupName);
    }

    private ConfigurationSection resolvePermissionSection(ConfigurationSection groupSection, String permissionGroupName) {
        return GroupSpawnConfigReader.findChildSectionIgnoreCase(groupSection, permissionGroupName);
    }

    private YamlConfiguration loadGroupsConfig() {
        return YamlConfiguration.loadConfiguration(groupsFile());
    }

    private YamlConfiguration loadRuntimeGroupsConfig() {
        return Objects.requireNonNull(runtimeGroupsConfigSupplier.get(), "runtimeGroupsConfigSupplier.get()");
    }

    private File groupsFile() {
        return new File(plugin.getDataFolder(), "groups.yml");
    }

    private void save(YamlConfiguration groupsConfig) {
        File groupsFile = groupsFile();
        File parentDirectory = groupsFile.getParentFile();
        if (parentDirectory != null && !parentDirectory.exists()) {
            parentDirectory.mkdirs();
        }

        try {
            groupsConfig.save(groupsFile);
        } catch (IOException exception) {
            throw new IllegalStateException("无法保存 groups.yml。", exception);
        }
    }

    private void saveValidated(YamlConfiguration groupsConfig) {
        GroupConfigValidator.validate(groupsConfig, loadConfigFromDisk(), plugin.getServer());
        save(groupsConfig);
    }

    private YamlConfiguration loadConfigFromDisk() {
        return YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "config.yml"));
    }

    private String requireName(String name, String fieldName) {
        String value = Objects.requireNonNull(name, fieldName);
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "不能为空。");
        }
        return value;
    }
}
