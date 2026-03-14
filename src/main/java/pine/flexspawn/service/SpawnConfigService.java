package pine.flexspawn.service;

// 用法：读取并校验各出生场景开关、坐标组引用以及个人存档点接管配置。
import pine.flexspawn.model.ScenarioSpawnConfig;
import pine.flexspawn.model.SpawnScenario;
import pine.flexspawn.model.GroupSpawnReference;
import pine.flexspawn.util.GroupSpawnConfigReader;
import java.io.File;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class SpawnConfigService {

    private static final String SPAWN_UPDATE_MESSAGE_PATH = "spawn-update-message";
    private static final String PERSONAL_SPAWN_PATH = "personal-spawn";
    private static final String DEFAULT_SPAWN_UPDATE_CHAT_MESSAGE = "已设置重生点";
    private static final String DEFAULT_SPAWN_UPDATE_ACTION_BAR_MESSAGE = "已设置重生点";
    private static final String DEFAULT_NO_PERMISSION_MESSAGE = "你没有权限";

    private final JavaPlugin plugin;
    private final Map<SpawnScenario, ScenarioSpawnConfig> scenarioConfigs;
    private final boolean personalSpawnEnabled;
    private final YamlConfiguration groupsConfig;

    public SpawnConfigService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.scenarioConfigs = new EnumMap<>(SpawnScenario.class);
        scenarioConfigs.put(SpawnScenario.FIRST_JOIN, readScenarioConfig(SpawnScenario.FIRST_JOIN));
        scenarioConfigs.put(SpawnScenario.JOIN_DEFAULT, readScenarioConfig(SpawnScenario.JOIN_DEFAULT));
        scenarioConfigs.put(SpawnScenario.DEATH_RESPAWN, readScenarioConfig(SpawnScenario.DEATH_RESPAWN));
        this.personalSpawnEnabled = plugin.getConfig().getBoolean(PERSONAL_SPAWN_PATH + ".enabled", true);
        File groupsFile = new File(plugin.getDataFolder(), "groups.yml");
        this.groupsConfig = YamlConfiguration.loadConfiguration(groupsFile);
    }

    public ScenarioSpawnConfig getScenarioConfig(SpawnScenario scenario) {
        return scenarioConfigs.get(scenario);
    }

    public boolean isPersonalSpawnEnabled() {
        return personalSpawnEnabled;
    }

    public Optional<String> getSpawnUpdateChatMessage() {
        return getOptionalMessage(
                SPAWN_UPDATE_MESSAGE_PATH + ".chat",
                DEFAULT_SPAWN_UPDATE_CHAT_MESSAGE
        );
    }

    public Optional<String> getSpawnUpdateActionBarMessage() {
        return getOptionalMessage(
                SPAWN_UPDATE_MESSAGE_PATH + ".action-bar",
                DEFAULT_SPAWN_UPDATE_ACTION_BAR_MESSAGE
        );
    }

    public String getNoPermissionMessage() {
        return plugin.getConfig().getString("no-permission-message", DEFAULT_NO_PERMISSION_MESSAGE);
    }

    public YamlConfiguration getGroupsConfig() {
        return groupsConfig;
    }

    public ConfigurationSection getRequiredGroupSection(String groupName) {
        ConfigurationSection groupSection = GroupSpawnConfigReader.findChildSectionIgnoreCase(groupsConfig, groupName);
        if (groupSection == null) {
            throw new IllegalStateException("未找到坐标组配置: " + groupName);
        }
        return groupSection;
    }

    public void validate() {
        GroupConfigValidator.validate(groupsConfig, plugin.getConfig(), plugin.getServer());
    }

    private ScenarioSpawnConfig readScenarioConfig(SpawnScenario scenario) {
        ConfigurationSection section = resolveScenarioSection(scenario.configPath());
        boolean enabled = section.getBoolean("enabled", true);
        if (!enabled) {
            return ScenarioSpawnConfig.disabled();
        }

        String group = section.getString("group");
        if (group == null || group.isBlank()) {
            throw new IllegalStateException("缺少坐标组名称配置: " + section.getCurrentPath() + ".group");
        }

        String point = section.getString("point");
        if (point != null && point.isBlank()) {
            point = null;
        }
        return new ScenarioSpawnConfig(true, new GroupSpawnReference(group, point));
    }

    private ConfigurationSection resolveScenarioSection(String path) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection(path);
        if (section != null) {
            return section;
        }
        throw new IllegalStateException("缺少出生场景配置节点: " + path);
    }

    private Optional<String> getOptionalMessage(String path, String defaultMessage) {
        if (!plugin.getConfig().getBoolean(path + ".enabled", true)) {
            return Optional.empty();
        }

        String message = plugin.getConfig().getString(path + ".text", defaultMessage);
        if (message == null || message.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(message);
    }

}
