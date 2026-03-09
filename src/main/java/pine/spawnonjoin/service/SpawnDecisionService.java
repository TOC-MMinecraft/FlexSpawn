package pine.spawnonjoin.service;

// 用法：集中处理首次加入、普通加入、复活和个人存档点更新规则。
import pine.spawnonjoin.model.LocationData;
import pine.spawnonjoin.repository.PlayerSpawnRepository;
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
    private final PlayerSpawnRepository repository;

    public SpawnDecisionService(
            JavaPlugin plugin,
            SpawnConfigService configService,
            PlayerSpawnRepository repository
    ) {
        this.plugin = plugin;
        this.configService = configService;
        this.repository = repository;
    }

    public Location resolveJoinLocation(UUID playerId) {
        if (!repository.exists(playerId)) {
            return configService.getFirstJoinLocation();
        }
        return resolveStoredOrDefault(playerId);
    }

    public Location resolveRespawnLocation(UUID playerId) {
        return resolveStoredOrDefault(playerId);
    }

    public void ensurePlayerRecord(UUID playerId, String playerName) {
        repository.ensurePlayer(playerId, playerName);
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

    private Location resolveStoredOrDefault(UUID playerId) {
        return loadStoredLocation(playerId).orElseGet(configService::getDefaultRespawnLocation);
    }

    private Optional<Location> loadStoredLocation(UUID playerId) {
        try {
            Optional<LocationData> locationData = repository.findPersonalSpawn(playerId);
            if (locationData.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(locationData.get().toLocation(plugin.getServer()));
        } catch (RuntimeException exception) {
            plugin.getLogger().log(
                    Level.WARNING,
                    "读取玩家个人存档点失败，将回退到默认点。playerId=" + playerId,
                    exception
            );
            return Optional.empty();
        }
    }
}
