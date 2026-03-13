package pine.flexspawn.service;

// 用法：解析玩家在某个公共出生场景下应使用的坐标点或拒绝结果。
import pine.flexspawn.model.GroupSpawnReference;
import pine.flexspawn.model.GroupSpawnSelectionResult;
import pine.flexspawn.model.WeightedLocationEntry;
import java.util.List;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public final class GroupSpawnResolver {

    private final SpawnConfigService configService;
    private final PermissionSpawnSelector permissionSpawnSelector;
    private final WeightedLocationPicker weightedLocationPicker;

    public GroupSpawnResolver(
            SpawnConfigService configService,
            PermissionSpawnSelector permissionSpawnSelector,
            WeightedLocationPicker weightedLocationPicker
    ) {
        this.configService = configService;
        this.permissionSpawnSelector = permissionSpawnSelector;
        this.weightedLocationPicker = weightedLocationPicker;
    }

    public GroupSpawnSelectionResult resolve(Player player, GroupSpawnReference reference) {
        ConfigurationSection groupSection = configService.getRequiredGroupSection(reference.group());
        List<WeightedLocationEntry> candidates = permissionSpawnSelector.collectCandidates(
                player,
                groupSection,
                reference.point()
        );
        if (candidates.isEmpty()) {
            return GroupSpawnSelectionResult.denied(configService.getNoPermissionMessage());
        }

        WeightedLocationEntry selected = weightedLocationPicker.pick(candidates);
        return GroupSpawnSelectionResult.success(selected.locationData());
    }
}
