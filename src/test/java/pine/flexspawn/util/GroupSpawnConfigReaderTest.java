package pine.flexspawn.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.junit.jupiter.api.Test;

class GroupSpawnConfigReaderTest {

    @Test
    void shouldFindGroupSectionIgnoringCase() {
        MemoryConfiguration root = new MemoryConfiguration();
        root.set("Join-Default.Default.point1.world", "world");
        root.set("Join-Default.Default.point1.x", 0.0D);
        root.set("Join-Default.Default.point1.y", 64.0D);
        root.set("Join-Default.Default.point1.z", 0.0D);
        root.set("Join-Default.Default.point1.yaw", 0.0D);
        root.set("Join-Default.Default.point1.pitch", 0.0D);
        root.set("Join-Default.Default.point1.weight", 1);

        ConfigurationSection groupSection = GroupSpawnConfigReader.findChildSectionIgnoreCase(root, "join-default");

        assertNotNull(groupSection);
        assertEquals("Join-Default", groupSection.getName());
    }

    @Test
    void shouldFindPointIgnoringCase() {
        MemoryConfiguration root = new MemoryConfiguration();
        root.set("join-default.Default.SpawnA.world", "world");
        root.set("join-default.Default.SpawnA.x", 0.0D);
        root.set("join-default.Default.SpawnA.y", 64.0D);
        root.set("join-default.Default.SpawnA.z", 0.0D);
        root.set("join-default.Default.SpawnA.yaw", 0.0D);
        root.set("join-default.Default.SpawnA.pitch", 0.0D);
        root.set("join-default.Default.SpawnA.weight", 1);

        ConfigurationSection groupSection = root.getConfigurationSection("join-default");

        assertTrue(GroupSpawnConfigReader.hasPoint(groupSection, "spawna"));
    }
}
