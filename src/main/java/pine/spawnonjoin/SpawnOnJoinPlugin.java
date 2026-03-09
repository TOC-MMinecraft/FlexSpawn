package pine.spawnonjoin;

// 用法：插件入口，负责初始化配置、仓储、服务和事件监听器。
import pine.spawnonjoin.listener.PlayerJoinSpawnListener;
import pine.spawnonjoin.listener.PlayerRespawnListener;
import pine.spawnonjoin.listener.PlayerSetSpawnListener;
import pine.spawnonjoin.repository.PlayerSpawnRepository;
import pine.spawnonjoin.repository.YamlPlayerSpawnRepository;
import pine.spawnonjoin.service.SpawnConfigService;
import pine.spawnonjoin.service.SpawnDecisionService;
import java.io.File;
import java.util.logging.Level;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class SpawnOnJoinPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        try {
            saveDefaultConfig();
            savePlayersFile();

            SpawnConfigService configService = new SpawnConfigService(this);
            configService.validate();

            PlayerSpawnRepository repository = new YamlPlayerSpawnRepository(this);
            SpawnDecisionService decisionService = new SpawnDecisionService(this, configService, repository);

            PluginManager pluginManager = getServer().getPluginManager();
            pluginManager.registerEvents(new PlayerJoinSpawnListener(decisionService), this);
            pluginManager.registerEvents(new PlayerRespawnListener(decisionService), this);
            pluginManager.registerEvents(new PlayerSetSpawnListener(decisionService), this);

            getLogger().info("SpawnOnJoin 已启用。");
        } catch (RuntimeException exception) {
            getLogger().log(Level.SEVERE, "SpawnOnJoin 初始化失败，插件将被禁用。", exception);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("SpawnOnJoin 已关闭。");
    }

    private void savePlayersFile() {
        File playersFile = new File(getDataFolder(), "players.yml");
        if (!playersFile.exists()) {
            saveResource("players.yml", false);
        }
    }
}
