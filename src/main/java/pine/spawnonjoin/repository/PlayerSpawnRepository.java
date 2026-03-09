package pine.spawnonjoin.repository;

// 用法：抽象玩家个人存档点数据源，便于未来替换为其他持久化实现。
import pine.spawnonjoin.model.LocationData;
import java.util.Optional;
import java.util.UUID;

public interface PlayerSpawnRepository {

    boolean exists(UUID playerId);

    void ensurePlayer(UUID playerId, String playerName);

    Optional<LocationData> findPersonalSpawn(UUID playerId);

    void savePersonalSpawn(UUID playerId, String playerName, LocationData locationData);

    void clearPersonalSpawn(UUID playerId, String playerName);
}
