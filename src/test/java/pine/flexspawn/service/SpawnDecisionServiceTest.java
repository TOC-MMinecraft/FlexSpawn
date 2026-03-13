package pine.flexspawn.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import pine.flexspawn.model.GroupSpawnReference;
import pine.flexspawn.model.GroupSpawnSelectionResult;
import pine.flexspawn.model.LocationData;
import pine.flexspawn.model.ScenarioSpawnConfig;
import pine.flexspawn.model.SpawnScenario;
import pine.flexspawn.repository.PlayerSpawnRepository;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;

class SpawnDecisionServiceTest {

    private static final GroupSpawnReference FIRST_JOIN_REFERENCE = new GroupSpawnReference("first-join", null);
    private static final GroupSpawnReference JOIN_DEFAULT_REFERENCE = new GroupSpawnReference("join-default", null);
    private static final GroupSpawnReference DEATH_RESPAWN_REFERENCE = new GroupSpawnReference("death-respawn", null);

    @Test
    void shouldUseFirstJoinScenarioWhenPlayerHasNoRecordAndFirstJoinEnabled() {
        PlayerSpawnRepository repository = mock(PlayerSpawnRepository.class);
        GroupSpawnResolver resolver = mock(GroupSpawnResolver.class);
        SpawnConfigService configService = mockConfigService(true, true, true, true);
        Player player = mockPlayer();
        GroupSpawnSelectionResult expected = GroupSpawnSelectionResult.success(location("first"));

        when(repository.exists(player.getUniqueId())).thenReturn(false);
        when(resolver.resolve(player, FIRST_JOIN_REFERENCE)).thenReturn(expected);

        Optional<GroupSpawnSelectionResult> result = createService(repository, resolver, configService).resolveJoinLocation(player);

        assertTrue(result.isPresent());
        assertEquals(expected, result.get());
        verify(resolver).resolve(player, FIRST_JOIN_REFERENCE);
        verify(resolver, never()).resolve(player, JOIN_DEFAULT_REFERENCE);
    }

    @Test
    void shouldFallbackToJoinDefaultWhenFirstJoinDisabled() {
        PlayerSpawnRepository repository = mock(PlayerSpawnRepository.class);
        GroupSpawnResolver resolver = mock(GroupSpawnResolver.class);
        SpawnConfigService configService = mockConfigService(false, true, true, true);
        Player player = mockPlayer();
        GroupSpawnSelectionResult expected = GroupSpawnSelectionResult.success(location("join"));

        when(repository.exists(player.getUniqueId())).thenReturn(false);
        when(resolver.resolve(player, JOIN_DEFAULT_REFERENCE)).thenReturn(expected);

        Optional<GroupSpawnSelectionResult> result = createService(repository, resolver, configService).resolveJoinLocation(player);

        assertTrue(result.isPresent());
        assertEquals(expected, result.get());
        verify(resolver).resolve(player, JOIN_DEFAULT_REFERENCE);
    }

    @Test
    void shouldNotManageFirstJoinWhenFirstJoinAndJoinDefaultAreDisabled() {
        PlayerSpawnRepository repository = mock(PlayerSpawnRepository.class);
        GroupSpawnResolver resolver = mock(GroupSpawnResolver.class);
        SpawnConfigService configService = mockConfigService(false, false, true, true);
        Player player = mockPlayer();

        when(repository.exists(player.getUniqueId())).thenReturn(false);

        Optional<GroupSpawnSelectionResult> result = createService(repository, resolver, configService).resolveJoinLocation(player);

        assertTrue(result.isEmpty());
        verify(resolver, never()).resolve(any(Player.class), any(GroupSpawnReference.class));
    }

    @Test
    void shouldUseStoredSpawnBeforeJoinDefaultForReturningPlayer() {
        PlayerSpawnRepository repository = mock(PlayerSpawnRepository.class);
        GroupSpawnResolver resolver = mock(GroupSpawnResolver.class);
        SpawnConfigService configService = mockConfigService(true, true, true, true);
        Player player = mockPlayer();
        LocationData stored = location("stored");

        when(repository.exists(player.getUniqueId())).thenReturn(true);
        when(repository.findPersonalSpawn(player.getUniqueId())).thenReturn(Optional.of(stored));

        Optional<GroupSpawnSelectionResult> result = createService(repository, resolver, configService).resolveJoinLocation(player);

        assertTrue(result.isPresent());
        assertTrue(result.get().isSuccess());
        assertEquals(stored, result.get().locationData());
        verify(resolver, never()).resolve(any(Player.class), any(GroupSpawnReference.class));
    }

    @Test
    void shouldFallbackToJoinDefaultWhenPersonalSpawnDisabled() {
        PlayerSpawnRepository repository = mock(PlayerSpawnRepository.class);
        GroupSpawnResolver resolver = mock(GroupSpawnResolver.class);
        SpawnConfigService configService = mockConfigService(true, true, true, false);
        Player player = mockPlayer();
        GroupSpawnSelectionResult expected = GroupSpawnSelectionResult.success(location("join"));

        when(repository.exists(player.getUniqueId())).thenReturn(true);
        when(resolver.resolve(player, JOIN_DEFAULT_REFERENCE)).thenReturn(expected);

        Optional<GroupSpawnSelectionResult> result = createService(repository, resolver, configService).resolveJoinLocation(player);

        assertTrue(result.isPresent());
        assertEquals(expected, result.get());
        verify(repository, never()).findPersonalSpawn(player.getUniqueId());
        verify(resolver).resolve(player, JOIN_DEFAULT_REFERENCE);
    }

    @Test
    void shouldFallbackToDeathRespawnWhenNoStoredSpawnExists() {
        PlayerSpawnRepository repository = mock(PlayerSpawnRepository.class);
        GroupSpawnResolver resolver = mock(GroupSpawnResolver.class);
        SpawnConfigService configService = mockConfigService(true, true, true, true);
        Player player = mockPlayer();
        GroupSpawnSelectionResult expected = GroupSpawnSelectionResult.success(location("respawn"));

        when(repository.findPersonalSpawn(player.getUniqueId())).thenReturn(Optional.empty());
        when(resolver.resolve(player, DEATH_RESPAWN_REFERENCE)).thenReturn(expected);

        Optional<GroupSpawnSelectionResult> result = createService(repository, resolver, configService).resolveRespawnLocation(player);

        assertTrue(result.isPresent());
        assertEquals(expected, result.get());
        verify(resolver).resolve(player, DEATH_RESPAWN_REFERENCE);
    }

    private SpawnDecisionService createService(
            PlayerSpawnRepository repository,
            GroupSpawnResolver resolver,
            SpawnConfigService configService
    ) {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("SpawnDecisionServiceTest"));
        return new SpawnDecisionService(plugin, configService, resolver, repository);
    }

    private SpawnConfigService mockConfigService(
            boolean firstJoinEnabled,
            boolean joinDefaultEnabled,
            boolean deathRespawnEnabled,
            boolean personalSpawnEnabled
    ) {
        SpawnConfigService configService = mock(SpawnConfigService.class);
        when(configService.getScenarioConfig(SpawnScenario.FIRST_JOIN))
                .thenReturn(firstJoinEnabled ? new ScenarioSpawnConfig(true, FIRST_JOIN_REFERENCE) : ScenarioSpawnConfig.disabled());
        when(configService.getScenarioConfig(SpawnScenario.JOIN_DEFAULT))
                .thenReturn(joinDefaultEnabled ? new ScenarioSpawnConfig(true, JOIN_DEFAULT_REFERENCE) : ScenarioSpawnConfig.disabled());
        when(configService.getScenarioConfig(SpawnScenario.DEATH_RESPAWN))
                .thenReturn(deathRespawnEnabled ? new ScenarioSpawnConfig(true, DEATH_RESPAWN_REFERENCE) : ScenarioSpawnConfig.disabled());
        when(configService.isPersonalSpawnEnabled()).thenReturn(personalSpawnEnabled);
        return configService;
    }

    private Player mockPlayer() {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
        return player;
    }

    private LocationData location(String worldName) {
        return new LocationData(worldName, 1.0, 64.0, 2.0, 0.0F, 0.0F);
    }
}
