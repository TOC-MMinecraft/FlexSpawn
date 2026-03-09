package pine.spawnonjoin.service;

// 用法：读取并校验公共点位配置，对外提供首次加入点和默认点。
import pine.spawnonjoin.model.LocationData;
import pine.spawnonjoin.util.LocationSerializer;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

public final class SpawnConfigService {

    private final JavaPlugin plugin;
    private final LocationData firstJoinLocationData;
    private final LocationData defaultRespawnLocationData;

    public SpawnConfigService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.firstJoinLocationData = LocationSerializer.readRequired(plugin.getConfig(), "first-join");
        this.defaultRespawnLocationData = LocationSerializer.readRequired(plugin.getConfig(), "default-respawn");
    }

    public Location getFirstJoinLocation() {
        return firstJoinLocationData.toLocation(plugin.getServer());
    }

    public Location getDefaultRespawnLocation() {
        return defaultRespawnLocationData.toLocation(plugin.getServer());
    }

    public void validate() {
        getFirstJoinLocation();
        getDefaultRespawnLocation();
    }
}
