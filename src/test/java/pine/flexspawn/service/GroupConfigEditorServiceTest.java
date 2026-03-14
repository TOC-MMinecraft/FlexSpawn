package pine.flexspawn.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import pine.flexspawn.model.LocationData;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GroupConfigEditorServiceTest {

    @TempDir
    Path tempDirectory;

    @Test
    void shouldAddCoordinateWithWeight() throws IOException {
        writeGroups("""
                join-default:
                  Default:
                    point1:
                      world: world
                      x: 0.0
                      y: 64.0
                      z: 0.0
                      yaw: 0.0
                      pitch: 0.0
                      weight: 1
                """);

        GroupConfigEditorService service = createService();

        boolean added = service.addCoordinate(
                "join-default",
                "Default",
                "point2",
                new LocationData("world", 10.0, 70.0, 5.0, 90.0F, 0.0F),
                3
        );

        assertTrue(added);

        YamlConfiguration groupsConfig = loadGroups();
        assertEquals("world", groupsConfig.getString("join-default.Default.point2.world"));
        assertEquals(3, groupsConfig.getInt("join-default.Default.point2.weight"));
    }

    @Test
    void shouldDeletePermissionGroupWhenAnotherPermissionGroupExists() throws IOException {
        writeGroups("""
                arena:
                  Default:
                    point1:
                      world: world
                      x: 0.0
                      y: 64.0
                      z: 0.0
                      yaw: 0.0
                      pitch: 0.0
                      weight: 1
                  vip:
                    point2:
                      world: world
                      x: 1.0
                      y: 65.0
                      z: 1.0
                      yaw: 0.0
                      pitch: 0.0
                      weight: 2
                """);

        GroupConfigEditorService service = createService();

        boolean deleted = service.deletePermissionGroup("arena", "vip");

        assertTrue(deleted);
        assertFalse(loadGroups().contains("arena.vip"));
    }

    @Test
    void shouldRejectDeletingGroupReferencedByConfig() throws IOException {
        writeConfig("""
                custom-route:
                  enabled: true
                  group: arena
                """);
        writeGroups("""
                arena:
                  Default:
                    point1:
                      world: world
                      x: 0.0
                      y: 64.0
                      z: 0.0
                      yaw: 0.0
                      pitch: 0.0
                      weight: 1
                lobby:
                  Default:
                    point2:
                      world: world
                      x: 1.0
                      y: 65.0
                      z: 1.0
                      yaw: 0.0
                      pitch: 0.0
                      weight: 1
                """);

        GroupConfigEditorService service = createService();

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.deleteGroup("arena")
        );

        assertEquals("配置节点引用了不存在的坐标组: custom-route -> arena", exception.getMessage());
        assertTrue(loadGroups().contains("arena"));
    }

    @Test
    void shouldRejectDeletingLastCoordinateInPermissionGroup() throws IOException {
        writeGroups("""
                arena:
                  Default:
                    point1:
                      world: world
                      x: 0.0
                      y: 64.0
                      z: 0.0
                      yaw: 0.0
                      pitch: 0.0
                      weight: 1
                """);

        GroupConfigEditorService service = createService();

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.deleteCoordinate("arena", "Default", "point1")
        );

        assertEquals("权限组至少需要保留一个坐标点，请直接删除整个权限组。", exception.getMessage());
    }

    @Test
    void shouldRejectDeletingCoordinateReferencedByConfigPoint() throws IOException {
        writeConfig("""
                custom-route:
                  enabled: true
                  group: arena
                  point: point1
                """);
        writeGroups("""
                arena:
                  Default:
                    point1:
                      world: world
                      x: 0.0
                      y: 64.0
                      z: 0.0
                      yaw: 0.0
                      pitch: 0.0
                      weight: 1
                    point2:
                      world: world
                      x: 2.0
                      y: 66.0
                      z: 2.0
                      yaw: 0.0
                      pitch: 0.0
                      weight: 1
                """);

        GroupConfigEditorService service = createService();

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.deleteCoordinate("arena", "Default", "point1")
        );

        assertEquals("坐标组中不存在指定坐标点: custom-route -> arena.point1", exception.getMessage());
        assertTrue(loadGroups().contains("arena.Default.point1"));
    }

    @Test
    void shouldValidateAgainstConfigFileOnDiskInsteadOfStaleInMemoryConfig() throws IOException {
        writeConfig("""
                custom-route:
                  enabled: true
                  group: arena
                """);
        writeGroups("""
                arena:
                  Default:
                    point1:
                      world: world
                      x: 0.0
                      y: 64.0
                      z: 0.0
                      yaw: 0.0
                      pitch: 0.0
                      weight: 1
                lobby:
                  Default:
                    point2:
                      world: world
                      x: 1.0
                      y: 65.0
                      z: 1.0
                      yaw: 0.0
                      pitch: 0.0
                      weight: 1
                """);

        GroupConfigEditorService service = createService(new YamlConfiguration());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.deleteGroup("arena")
        );

        assertEquals("配置节点引用了不存在的坐标组: custom-route -> arena", exception.getMessage());
        assertTrue(loadGroups().contains("arena"));
    }

    @Test
    void shouldFindCoordinateIgnoringCase() throws IOException {
        writeGroups("""
                arena:
                  Default:
                    SpawnA:
                      world: world
                      x: 12.0
                      y: 70.0
                      z: 8.0
                      yaw: 180.0
                      pitch: 0.0
                      weight: 1
                """);

        GroupConfigEditorService service = createService();

        LocationData locationData = service.findCoordinateLocation("arena", "spawna").orElseThrow();

        assertEquals("world", locationData.world());
        assertEquals(12.0, locationData.x());
        assertEquals(180.0F, locationData.yaw());
    }

    @Test
    void shouldUseDefaultPermissionGroupWhenPlayerHasNoSpawnGroupPermission() throws IOException {
        writeGroups("""
                arena:
                  Default:
                    spawn-default:
                      world: world
                      x: 12.0
                      y: 70.0
                      z: 8.0
                      yaw: 180.0
                      pitch: 0.0
                      weight: 1
                  vip:
                    spawn-vip:
                      world: world
                      x: 88.0
                      y: 75.0
                      z: 42.0
                      yaw: 0.0
                      pitch: 0.0
                      weight: 1
                """);

        GroupConfigEditorService service = createService();
        Player player = mock(Player.class);
        when(player.hasPermission("flexspawn.group.vip")).thenReturn(false);

        LocationData locationData = service.findGroupLocation("arena", player).orElseThrow();

        assertEquals(12.0, locationData.x());
        assertNotEquals(88.0, locationData.x());
    }

    @Test
    void shouldUseMatchedPermissionGroupWhenPlayerHasSpawnGroupPermission() throws IOException {
        writeGroups("""
                arena:
                  Default:
                    spawn-default:
                      world: world
                      x: 12.0
                      y: 70.0
                      z: 8.0
                      yaw: 180.0
                      pitch: 0.0
                      weight: 1
                  vip:
                    spawn-vip:
                      world: world
                      x: 88.0
                      y: 75.0
                      z: 42.0
                      yaw: 0.0
                      pitch: 0.0
                      weight: 1
                """);

        GroupConfigEditorService service = createService();
        Player player = mock(Player.class);
        when(player.hasPermission("flexspawn.group.vip")).thenReturn(true);

        LocationData locationData = service.findGroupLocation("arena", player).orElseThrow();

        assertEquals(88.0, locationData.x());
        assertNotEquals(12.0, locationData.x());
    }

    @Test
    void shouldUseRuntimeGroupsForQueriesWhenDiskConfigDiverges() throws IOException {
        writeGroups("""
                disk-group:
                  Default:
                    disk-point:
                      world: world
                      x: 1.0
                      y: 65.0
                      z: 1.0
                      yaw: 0.0
                      pitch: 0.0
                      weight: 1
                """);

        YamlConfiguration runtimeGroupsConfig = parseYaml("""
                runtime-group:
                  Default:
                    runtime-point:
                      world: world
                      x: 12.0
                      y: 70.0
                      z: 8.0
                      yaw: 180.0
                      pitch: 0.0
                      weight: 1
                """);

        GroupConfigEditorService service = createService(loadConfig(), runtimeGroupsConfig);

        assertEquals(List.of("runtime-group"), service.getGroupNames());
        assertTrue(service.findCoordinateLocation("runtime-group", "runtime-point").isPresent());
        assertFalse(service.findCoordinateLocation("disk-group", "disk-point").isPresent());
    }

    private GroupConfigEditorService createService() {
        return createService(loadConfig());
    }

    private GroupConfigEditorService createService(YamlConfiguration inMemoryConfig) {
        return createService(inMemoryConfig, loadGroups());
    }

    private GroupConfigEditorService createService(
            YamlConfiguration inMemoryConfig,
            YamlConfiguration runtimeGroupsConfig
    ) {
        return new GroupConfigEditorService(
                mockPlugin(inMemoryConfig),
                new PermissionSpawnSelector(),
                new WeightedLocationPicker(),
                () -> runtimeGroupsConfig
        );
    }

    private JavaPlugin mockPlugin(YamlConfiguration inMemoryConfig) {
        JavaPlugin plugin = mock(JavaPlugin.class);
        Server server = mock(Server.class);
        World world = mock(World.class);
        when(plugin.getDataFolder()).thenReturn(tempDirectory.toFile());
        when(plugin.getConfig()).thenReturn(inMemoryConfig);
        when(plugin.getServer()).thenReturn(server);
        when(server.getWorld(anyString())).thenReturn(world);
        return plugin;
    }

    private void writeConfig(String content) throws IOException {
        Files.writeString(tempDirectory.resolve("config.yml"), content);
    }

    private void writeGroups(String content) throws IOException {
        Files.writeString(tempDirectory.resolve("groups.yml"), content);
    }

    private YamlConfiguration loadConfig() {
        return YamlConfiguration.loadConfiguration(tempDirectory.resolve("config.yml").toFile());
    }

    private YamlConfiguration loadGroups() {
        return YamlConfiguration.loadConfiguration(tempDirectory.resolve("groups.yml").toFile());
    }

    private YamlConfiguration parseYaml(String content) {
        YamlConfiguration configuration = new YamlConfiguration();
        try {
            configuration.loadFromString(content);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to parse test YAML", exception);
        }
        return configuration;
    }
}
