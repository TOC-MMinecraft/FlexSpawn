package pine.flexspawn;

// 用法：插件入口，负责初始化配置、仓储、服务和事件监听器。
import pine.flexspawn.listener.PlayerJoinSpawnListener;
import pine.flexspawn.listener.PlayerRespawnListener;
import pine.flexspawn.listener.PlayerSetSpawnListener;
import pine.flexspawn.repository.PlayerSpawnRepository;
import pine.flexspawn.repository.YamlPlayerSpawnRepository;
import pine.flexspawn.service.GroupSpawnResolver;
import pine.flexspawn.service.PermissionSpawnSelector;
import pine.flexspawn.service.SpawnConfigService;
import pine.flexspawn.service.SpawnDecisionService;
import pine.flexspawn.service.WeightedLocationPicker;
import java.io.File;
import java.util.logging.Level;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class FlexSpawnPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        try {
            saveDefaultConfig();
            savePlayersFile();
            saveGroupsFile();

            SpawnConfigService configService = new SpawnConfigService(this);
            configService.validate();

            PlayerSpawnRepository repository = new YamlPlayerSpawnRepository(this);
            GroupSpawnResolver groupSpawnResolver = new GroupSpawnResolver(
                    configService,
                    new PermissionSpawnSelector(),
                    new WeightedLocationPicker()
            );
            SpawnDecisionService decisionService = new SpawnDecisionService(
                    this,
                    configService,
                    groupSpawnResolver,
                    repository
            );

            PluginManager pluginManager = getServer().getPluginManager();
            pluginManager.registerEvents(new PlayerJoinSpawnListener(decisionService), this);
            pluginManager.registerEvents(new PlayerRespawnListener(this, decisionService), this);
            pluginManager.registerEvents(new PlayerSetSpawnListener(decisionService), this);

            getLogger().info("FlexSpawn 已启用。");
        } catch (RuntimeException exception) {
            getLogger().log(Level.SEVERE, "FlexSpawn 初始化失败，插件将被禁用。", exception);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("FlexSpawn 已关闭。");
    }

    private void savePlayersFile() {
        File playersFile = new File(getDataFolder(), "players.yml");
        if (!playersFile.exists()) {
            saveResource("players.yml", false);
        }
    }

    private void saveGroupsFile() {
        File groupsFile = new File(getDataFolder(), "groups.yml");
        if (!groupsFile.exists()) {
            saveResource("groups.yml", false);
        }
    }
}
