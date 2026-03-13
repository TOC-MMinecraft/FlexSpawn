package pine.flexspawn.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class GroupsYamlRegressionTest {

    @Test
    void shouldExposeYamlDotParsingBehavior() throws Exception {
        String yaml = """
                join-default:
                  flexspawn.group.vip:
                    point1:
                      world: world
                      x: 1
                      y: 2
                      z: 3
                      yaw: 0
                      pitch: 0
                      weight: 1
                """;

        YamlConfiguration configuration = new YamlConfiguration();
        configuration.loadFromString(yaml);

        ConfigurationSection section = configuration.getConfigurationSection("join-default");
        assertNotNull(section);
        assertTrue(section.getKeys(false).contains("flexspawn"));
        assertFalse(section.getKeys(false).contains("flexspawn.group.vip"));
        assertNotNull(section.getConfigurationSection("flexspawn.group.vip"));
        assertNotNull(section.getConfigurationSection("flexspawn"));
    }

    @Test
    void shouldLoadBundledGroupsFileWithExpectedPermissionGroupKeys() {
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(
                Path.of("src", "main", "resources", "groups.yml").toFile()
        );

        ConfigurationSection section = configuration.getConfigurationSection("join-default");
        assertNotNull(section);
        assertTrue(section.getKeys(false).contains("Default"));
        assertTrue(section.getKeys(false).contains("vip"));
    }

    @Test
    void shouldExposeWorldForEveryConfiguredPointInBundledGroupsFile() {
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(
                Path.of("src", "main", "resources", "groups.yml").toFile()
        );

        for (String groupName : configuration.getKeys(false)) {
            ConfigurationSection groupSection = configuration.getConfigurationSection(groupName);
            assertNotNull(groupSection);
            for (String permissionGroup : groupSection.getKeys(false)) {
                ConfigurationSection permissionSection = groupSection.getConfigurationSection(permissionGroup);
                assertNotNull(permissionSection);
                for (String pointName : permissionSection.getKeys(false)) {
                    ConfigurationSection pointSection = permissionSection.getConfigurationSection(pointName);
                    assertNotNull(pointSection);
                    assertNotNull(pointSection.getString("world"));
                }
            }
        }
    }
}