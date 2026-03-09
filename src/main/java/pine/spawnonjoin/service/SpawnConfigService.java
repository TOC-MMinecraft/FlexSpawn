package pine.spawnonjoin.service;

// 用法：读取并校验公共点位配置，对外提供首次加入点和默认点。
import pine.spawnonjoin.model.LocationData;
import pine.spawnonjoin.util.LocationSerializer;
import java.util.Optional;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

public final class SpawnConfigService {

    private static final String SPAWN_UPDATE_MESSAGE_PATH = "spawn-update-message";
    private static final String DEFAULT_SPAWN_UPDATE_CHAT_MESSAGE = "已设置重生点";
    private static final String DEFAULT_SPAWN_UPDATE_ACTION_BAR_MESSAGE = "已设置重生点";

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

    public void validate() {
        getFirstJoinLocation();
        getDefaultRespawnLocation();
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
