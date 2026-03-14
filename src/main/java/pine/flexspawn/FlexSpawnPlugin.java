package pine.flexspawn;

// 用法：作为插件入口初始化运行时、监听器和命令，并支持热重载。
import pine.flexspawn.command.FlexSpawnCommand;
import pine.flexspawn.command.FlexSpawnTabCompleter;
import pine.flexspawn.listener.PlayerJoinSpawnListener;
import pine.flexspawn.listener.PlayerRespawnListener;
import pine.flexspawn.listener.PlayerSetSpawnListener;
import pine.flexspawn.runtime.PluginRuntime;
import pine.flexspawn.service.CommandPermissionService;
import pine.flexspawn.service.GroupConfigEditorService;
import pine.flexspawn.service.PermissionSpawnSelector;
import pine.flexspawn.service.SpawnDecisionService;
import pine.flexspawn.service.WeightedLocationPicker;
import java.io.File;
import java.util.Objects;
import java.util.logging.Level;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class FlexSpawnPlugin extends JavaPlugin {

    private PluginRuntime runtime;
    private GroupConfigEditorService groupConfigEditorService;
    private CommandPermissionService commandPermissionService;

    @Override
    public void onEnable() {
        try {
            saveDefaultConfig();
            savePlayersFile();
            saveGroupsFile();

            groupConfigEditorService = new GroupConfigEditorService(
                    this,
                    new PermissionSpawnSelector(),
                    new WeightedLocationPicker(),
                    () -> getRuntime().configService().getGroupsConfig()
            );
            commandPermissionService = new CommandPermissionService();
            reloadRuntime();

            PluginManager pluginManager = getServer().getPluginManager();
            pluginManager.registerEvents(new PlayerJoinSpawnListener(this::getDecisionService), this);
            pluginManager.registerEvents(new PlayerRespawnListener(this, this::getDecisionService), this);
            pluginManager.registerEvents(new PlayerSetSpawnListener(this::getDecisionService), this);
            registerCommands();

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

    public void reloadRuntime() {
        reloadConfig();
        runtime = PluginRuntime.create(this);
    }

    public PluginRuntime getRuntime() {
        return Objects.requireNonNull(runtime, "runtime");
    }

    public GroupConfigEditorService getGroupConfigEditorService() {
        return Objects.requireNonNull(groupConfigEditorService, "groupConfigEditorService");
    }

    private SpawnDecisionService getDecisionService() {
        return getRuntime().spawnDecisionService();
    }

    private void registerCommands() {
        PluginCommand flexSpawnCommand = Objects.requireNonNull(
                getCommand("flexspawn"),
                "plugin.yml 中缺少 flexspawn 命令定义。"
        );
        FlexSpawnCommand commandExecutor = new FlexSpawnCommand(
                this,
                getGroupConfigEditorService(),
                commandPermissionService
        );
        flexSpawnCommand.setExecutor(commandExecutor);
        flexSpawnCommand.setTabCompleter(new FlexSpawnTabCompleter(this, getGroupConfigEditorService()));
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
