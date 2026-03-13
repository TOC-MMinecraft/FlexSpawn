package pine.flexspawn.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import pine.flexspawn.model.WeightedLocationEntry;
import java.util.List;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

class PermissionSpawnSelectorTest {

    private final PermissionSpawnSelector selector = new PermissionSpawnSelector();

    @Test
    void shouldUsePrefixedPermissionNodeForPermissionGroups() {
        ConfigurationSection groupSection = createGroupSection();
        Player player = mock(Player.class);
        when(player.hasPermission("flexspawn.group.vip")).thenReturn(true);

        List<WeightedLocationEntry> candidates = selector.collectCandidates(player, groupSection, null);

        assertEquals(1, candidates.size());
        assertEquals("VIP", candidates.get(0).permissionGroup());
        verify(player).hasPermission("flexspawn.group.vip");
        verify(player, never()).hasPermission("VIP");
    }

    @Test
    void shouldFallbackToDefaultWhenNoPermissionGroupMatches() {
        ConfigurationSection groupSection = createGroupSection();
        Player player = mock(Player.class);
        when(player.hasPermission("flexspawn.group.vip")).thenReturn(false);

        List<WeightedLocationEntry> candidates = selector.collectCandidates(player, groupSection, "POINT1");

        assertEquals(1, candidates.size());
        assertEquals("Default", candidates.get(0).permissionGroup());
        assertEquals("point1", candidates.get(0).pointName());
    }

    @Test
    void shouldFallbackToLowercaseDefaultWhenNoPermissionGroupMatches() {
        MemoryConfiguration root = new MemoryConfiguration();
        root.set("join-default.default.point1.world", "world");
        root.set("join-default.default.point1.x", 10.0D);
        root.set("join-default.default.point1.y", 64.0D);
        root.set("join-default.default.point1.z", 10.0D);
        root.set("join-default.default.point1.yaw", 0.0D);
        root.set("join-default.default.point1.pitch", 0.0D);
        root.set("join-default.default.point1.weight", 1);
        root.set("join-default.vip.point1.world", "world");
        root.set("join-default.vip.point1.x", 20.0D);
        root.set("join-default.vip.point1.y", 64.0D);
        root.set("join-default.vip.point1.z", 20.0D);
        root.set("join-default.vip.point1.yaw", 0.0D);
        root.set("join-default.vip.point1.pitch", 0.0D);
        root.set("join-default.vip.point1.weight", 1);

        ConfigurationSection groupSection = root.getConfigurationSection("join-default");
        Player player = mock(Player.class);
        when(player.hasPermission("flexspawn.group.vip")).thenReturn(false);

        List<WeightedLocationEntry> candidates = selector.collectCandidates(player, groupSection, "point1");

        assertEquals(1, candidates.size());
        assertEquals("default", candidates.get(0).permissionGroup());
    }

    @Test
    void shouldDenyWhenMatchedPermissionGroupMissesFixedPoint() {
        MemoryConfiguration root = new MemoryConfiguration();
        root.set("join-default.Default.point1.world", "world");
        root.set("join-default.Default.point1.x", 10.0D);
        root.set("join-default.Default.point1.y", 64.0D);
        root.set("join-default.Default.point1.z", 10.0D);
        root.set("join-default.Default.point1.yaw", 0.0D);
        root.set("join-default.Default.point1.pitch", 0.0D);
        root.set("join-default.Default.point1.weight", 1);
        root.set("join-default.vip.other.world", "world");
        root.set("join-default.vip.other.x", 20.0D);
        root.set("join-default.vip.other.y", 64.0D);
        root.set("join-default.vip.other.z", 20.0D);
        root.set("join-default.vip.other.yaw", 0.0D);
        root.set("join-default.vip.other.pitch", 0.0D);
        root.set("join-default.vip.other.weight", 1);

        ConfigurationSection groupSection = root.getConfigurationSection("join-default");
        Player player = mock(Player.class);
        when(player.hasPermission("flexspawn.group.vip")).thenReturn(true);

        List<WeightedLocationEntry> candidates = selector.collectCandidates(player, groupSection, "point1");

        assertTrue(candidates.isEmpty());
    }

    private ConfigurationSection createGroupSection() {
        MemoryConfiguration root = new MemoryConfiguration();
        root.set("join-default.Default.point1.world", "world");
        root.set("join-default.Default.point1.x", 10.0D);
        root.set("join-default.Default.point1.y", 64.0D);
        root.set("join-default.Default.point1.z", 10.0D);
        root.set("join-default.Default.point1.yaw", 0.0D);
        root.set("join-default.Default.point1.pitch", 0.0D);
        root.set("join-default.Default.point1.weight", 1);
        root.set("join-default.VIP.point1.world", "world");
        root.set("join-default.VIP.point1.x", 20.0D);
        root.set("join-default.VIP.point1.y", 64.0D);
        root.set("join-default.VIP.point1.z", 20.0D);
        root.set("join-default.VIP.point1.yaw", 0.0D);
        root.set("join-default.VIP.point1.pitch", 0.0D);
        root.set("join-default.VIP.point1.weight", 1);
        return root.getConfigurationSection("join-default");
    }
}