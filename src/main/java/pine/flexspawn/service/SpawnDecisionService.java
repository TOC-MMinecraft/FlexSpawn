package pine.flexspawn.service;

// 用法：集中处理首次加入、普通加入、死亡重生与个人存档点接管规则。
import pine.flexspawn.model.GroupSpawnReference;
import pine.flexspawn.model.GroupSpawnSelectionResult;
import pine.flexspawn.model.LocationData;
import pine.flexspawn.model.ScenarioSpawnConfig;
import pine.flexspawn.model.SpawnScenario;
import pine.flexspawn.repository.PlayerSpawnRepository;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class SpawnDecisionService {

    private final JavaPlugin plugin;
    private final SpawnConfigService configService;
    private final GroupSpawnResolver groupSpawnResolver;
    private final PlayerSpawnRepository repository;

    public SpawnDecisionService(
            JavaPlugin plugin,
            SpawnConfigService configService,
            GroupSpawnResolver groupSpawnResolver,
            PlayerSpawnRepository repository
    ) {
        this.plugin = plugin;
        this.configService = configService;
        this.groupSpawnResolver = groupSpawnResolver;
        this.repository = repository;
    }

    public Optional<GroupSpawnSelectionResult> resolveJoinLocation(Player player) {
        UUID playerId = player.getUniqueId();
        if (!repository.exists(playerId)) {
            return resolveFirstJoinLocation(player);
        }
        return resolveStoredOrScenario(player, SpawnScenario.JOIN_DEFAULT);
    }

    public Optional<GroupSpawnSelectionResult> resolveRespawnLocation(Player player) {
        return resolveStoredOrScenario(player, SpawnScenario.DEATH_RESPAWN);
    }

    public void ensurePlayerRecord(UUID playerId, String playerName) {
        repository.ensurePlayer(playerId, playerName);
    }

    public boolean isPersonalSpawnManaged() {
        return configService.isPersonalSpawnEnabled();
    }

    public void recordPersonalSpawn(UUID playerId, String playerName, Location location) {
        repository.savePersonalSpawn(playerId, playerName, LocationData.fromLocation(location));
    }

    public void recordPersonalSpawnAndNotify(Player player, Location location) {
        recordPersonalSpawn(player.getUniqueId(), player.getName(), location);
        configService.getSpawnUpdateChatMessage().ifPresent(player::sendMessage);
        configService.getSpawnUpdateActionBarMessage()
                .ifPresent(message -> player.sendActionBar(Component.text(message)));
    }

    public void clearPersonalSpawn(UUID playerId, String playerName) {
        repository.clearPersonalSpawn(playerId, playerName);
    }

    private Optional<GroupSpawnSelectionResult> resolveFirstJoinLocation(Player player) {
        ScenarioSpawnConfig firstJoinConfig = configService.getScenarioConfig(SpawnScenario.FIRST_JOIN);
        if (firstJoinConfig.enabled()) {
            return Optional.of(resolveScenario(player, firstJoinConfig.requiredReference()));
        }
        return resolveScenarioIfEnabled(player, SpawnScenario.JOIN_DEFAULT);
    }

    private Optional<GroupSpawnSelectionResult> resolveStoredOrScenario(Player player, SpawnScenario fallbackScenario) {
        Optional<LocationData> storedLocation = loadStoredLocation(player.getUniqueId());
        if (storedLocation.isPresent()) {
            return Optional.of(GroupSpawnSelectionResult.success(storedLocation.get()));
        }
        return resolveScenarioIfEnabled(player, fallbackScenario);
    }

    private Optional<GroupSpawnSelectionResult> resolveScenarioIfEnabled(Player player, SpawnScenario scenario) {
        ScenarioSpawnConfig scenarioConfig = configService.getScenarioConfig(scenario);
        if (!scenarioConfig.enabled()) {
            return Optional.empty();
        }
        return Optional.of(resolveScenario(player, scenarioConfig.requiredReference()));
    }

    private GroupSpawnSelectionResult resolveScenario(Player player, GroupSpawnReference reference) {
        return groupSpawnResolver.resolve(player, reference);
    }

    private Optional<LocationData> loadStoredLocation(UUID playerId) {
        if (!configService.isPersonalSpawnEnabled()) {
            return Optional.empty();
        }

        try {
            return repository.findPersonalSpawn(playerId);
        } catch (RuntimeException exception) {
            plugin.getLogger().log(
                    Level.WARNING,
                    "读取玩家个人存档点失败，将继续回退公共出生点。playerId=" + playerId,
                    exception
            );
            return Optional.empty();
        }
    }
}