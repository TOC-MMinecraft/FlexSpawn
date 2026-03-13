package pine.flexspawn.service;

// 用法：根据玩家权限从某个坐标组中收集候选坐标点，支持 Default 兜底。
import pine.flexspawn.model.WeightedLocationEntry;
import pine.flexspawn.util.GroupSpawnConfigReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public final class PermissionSpawnSelector {

    private static final String DEFAULT_PERMISSION_GROUP = "Default";
    private static final String PERMISSION_NODE_PREFIX = "flexspawn.group.";

    public List<WeightedLocationEntry> collectCandidates(
            Player player,
            ConfigurationSection groupSection,
            String pointName
    ) {
        List<WeightedLocationEntry> candidates = new ArrayList<>();
        boolean matchedPermissionGroup = false;

        for (String permissionGroup : groupSection.getKeys(false)) {
            if (DEFAULT_PERMISSION_GROUP.equalsIgnoreCase(permissionGroup)) {
                continue;
            }
            if (!player.hasPermission(toPermissionNode(permissionGroup))) {
                continue;
            }

            matchedPermissionGroup = true;
            collectFromPermissionSection(groupSection, permissionGroup, pointName, candidates);
        }

        if (!candidates.isEmpty()) {
            return List.copyOf(candidates);
        }

        if (!matchedPermissionGroup) {
            String defaultPermissionGroup = findDefaultPermissionGroup(groupSection);
            if (defaultPermissionGroup != null) {
                collectFromPermissionSection(groupSection, defaultPermissionGroup, pointName, candidates);
            }
        }
        return List.copyOf(candidates);
    }

    private String toPermissionNode(String permissionGroup) {
        return PERMISSION_NODE_PREFIX + permissionGroup.toLowerCase(Locale.ROOT);
    }

    private String findDefaultPermissionGroup(ConfigurationSection groupSection) {
        for (String permissionGroup : groupSection.getKeys(false)) {
            if (DEFAULT_PERMISSION_GROUP.equalsIgnoreCase(permissionGroup)) {
                return permissionGroup;
            }
        }
        return null;
    }

    private void collectFromPermissionSection(
            ConfigurationSection groupSection,
            String permissionGroup,
            String pointName,
            List<WeightedLocationEntry> candidates
    ) {
        ConfigurationSection permissionSection = GroupSpawnConfigReader.findChildSectionIgnoreCase(
                groupSection,
                permissionGroup
        );
        if (permissionSection == null) {
            return;
        }

        if (pointName != null) {
            String resolvedPointName = GroupSpawnConfigReader.findKeyIgnoreCase(permissionSection, pointName);
            if (resolvedPointName != null) {
                candidates.add(GroupSpawnConfigReader.readWeightedLocationEntry(permissionSection, resolvedPointName));
            }
            return;
        }

        for (String availablePointName : permissionSection.getKeys(false)) {
            candidates.add(GroupSpawnConfigReader.readWeightedLocationEntry(permissionSection, availablePointName));
        }
    }
}
