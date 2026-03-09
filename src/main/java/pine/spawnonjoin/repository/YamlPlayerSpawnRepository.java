package pine.spawnonjoin.repository;

// 用法：将玩家 UUID、名称和个人存档点持久化到 players.yml。
import pine.spawnonjoin.model.LocationData;
import pine.spawnonjoin.util.LocationSerializer;
import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class YamlPlayerSpawnRepository implements PlayerSpawnRepository {

    private final File playersFile;
    private final YamlConfiguration playersConfig;

    public YamlPlayerSpawnRepository(JavaPlugin plugin) {
        this.playersFile = new File(plugin.getDataFolder(), "players.yml");
        this.playersConfig = YamlConfiguration.loadConfiguration(playersFile);
        playersRoot();
    }

    @Override
    public synchronized boolean exists(UUID playerId) {
        return playersRoot().isConfigurationSection(playerId.toString());
    }

    @Override
    public synchronized void ensurePlayer(UUID playerId, String playerName) {
        ConfigurationSection section = getPlayerSection(playerId);
        if (section == null) {
            section = playersRoot().createSection(playerId.toString());
            section.set("playername", playerName);
            save();
            return;
        }

        if (updatePlayerName(section, playerName)) {
            save();
        }
    }

    @Override
    public synchronized Optional<LocationData> findPersonalSpawn(UUID playerId) {
        ConfigurationSection section = getPlayerSection(playerId);
        if (section == null) {
            return Optional.empty();
        }
        return LocationSerializer.readOptional(section, "spawn");
    }

    @Override
    public synchronized void savePersonalSpawn(UUID playerId, String playerName, LocationData locationData) {
        ConfigurationSection section = getOrCreatePlayerSection(playerId);
        updatePlayerName(section, playerName);
        LocationSerializer.write(section, "spawn", locationData);
        save();
    }

    @Override
    public synchronized void clearPersonalSpawn(UUID playerId, String playerName) {
        ConfigurationSection section = getOrCreatePlayerSection(playerId);
        boolean changed = updatePlayerName(section, playerName);
        if (!section.contains("spawn")) {
            if (changed) {
                save();
            }
            return;
        }
        LocationSerializer.clear(section, "spawn");
        save();
    }

    private ConfigurationSection playersRoot() {
        ConfigurationSection section = playersConfig.getConfigurationSection("players");
        if (section == null) {
            section = playersConfig.createSection("players");
        }
        return section;
    }

    private ConfigurationSection getOrCreatePlayerSection(UUID playerId) {
        ConfigurationSection section = getPlayerSection(playerId);
        if (section != null) {
            return section;
        }
        return playersRoot().createSection(playerId.toString());
    }

    private ConfigurationSection getPlayerSection(UUID playerId) {
        return playersRoot().getConfigurationSection(playerId.toString());
    }

    private boolean updatePlayerName(ConfigurationSection section, String playerName) {
        String currentName = section.getString("playername", "");
        if (currentName.equals(playerName)) {
            return false;
        }
        section.set("playername", playerName);
        return true;
    }

    private void save() {
        try {
            playersConfig.save(playersFile);
        } catch (IOException exception) {
            throw new IllegalStateException("无法保存 players.yml。", exception);
        }
    }
}
