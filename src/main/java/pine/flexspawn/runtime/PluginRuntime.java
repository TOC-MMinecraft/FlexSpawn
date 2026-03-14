package pine.flexspawn.runtime;

// 用法：集中构建插件运行时依赖，便于启动和热重载时整体替换。
import pine.flexspawn.repository.PlayerSpawnRepository;
import pine.flexspawn.repository.YamlPlayerSpawnRepository;
import pine.flexspawn.service.GroupSpawnResolver;
import pine.flexspawn.service.PermissionSpawnSelector;
import pine.flexspawn.service.SpawnConfigService;
import pine.flexspawn.service.SpawnDecisionService;
import pine.flexspawn.service.WeightedLocationPicker;
import org.bukkit.plugin.java.JavaPlugin;

public final class PluginRuntime {

    private final SpawnConfigService configService;
    private final PlayerSpawnRepository playerSpawnRepository;
    private final GroupSpawnResolver groupSpawnResolver;
    private final SpawnDecisionService spawnDecisionService;

    private PluginRuntime(
            SpawnConfigService configService,
            PlayerSpawnRepository playerSpawnRepository,
            GroupSpawnResolver groupSpawnResolver,
            SpawnDecisionService spawnDecisionService
    ) {
        this.configService = configService;
        this.playerSpawnRepository = playerSpawnRepository;
        this.groupSpawnResolver = groupSpawnResolver;
        this.spawnDecisionService = spawnDecisionService;
    }

    public static PluginRuntime create(JavaPlugin plugin) {
        SpawnConfigService configService = new SpawnConfigService(plugin);
        configService.validate();

        PlayerSpawnRepository repository = new YamlPlayerSpawnRepository(plugin);
        GroupSpawnResolver groupSpawnResolver = new GroupSpawnResolver(
                configService,
                new PermissionSpawnSelector(),
                new WeightedLocationPicker()
        );
        SpawnDecisionService decisionService = new SpawnDecisionService(
                plugin,
                configService,
                groupSpawnResolver,
                repository
        );
        return new PluginRuntime(configService, repository, groupSpawnResolver, decisionService);
    }

    public SpawnConfigService configService() {
        return configService;
    }

    public PlayerSpawnRepository playerSpawnRepository() {
        return playerSpawnRepository;
    }

    public GroupSpawnResolver groupSpawnResolver() {
        return groupSpawnResolver;
    }

    public SpawnDecisionService spawnDecisionService() {
        return spawnDecisionService;
    }
}
