package pine.flexspawn.listener;

// 用法：处理玩家首次加入或普通加入时的落点，并在玩家成功进入后补齐档案记录。
import pine.flexspawn.model.GroupSpawnSelectionResult;
import pine.flexspawn.service.SpawnDecisionService;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

public final class PlayerJoinSpawnListener implements Listener {

    private final SpawnDecisionService decisionService;
    private final Map<UUID, String> pendingMessages = new ConcurrentHashMap<>();

    public PlayerJoinSpawnListener(SpawnDecisionService decisionService) {
        this.decisionService = decisionService;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerSpawnLocation(PlayerSpawnLocationEvent event) {
        Optional<GroupSpawnSelectionResult> result = decisionService.resolveJoinLocation(event.getPlayer());
        if (result.isEmpty()) {
            pendingMessages.remove(event.getPlayer().getUniqueId());
            return;
        }

        GroupSpawnSelectionResult selectionResult = result.get();
        if (selectionResult.isSuccess()) {
            pendingMessages.remove(event.getPlayer().getUniqueId());
            event.setSpawnLocation(selectionResult.locationData().toLocation(event.getPlayer().getServer()));
            return;
        }

        pendingMessages.put(event.getPlayer().getUniqueId(), selectionResult.denialMessage());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        decisionService.ensurePlayerRecord(
                event.getPlayer().getUniqueId(),
                event.getPlayer().getName()
        );

        String pendingMessage = pendingMessages.remove(event.getPlayer().getUniqueId());
        if (pendingMessage != null) {
            event.getPlayer().sendMessage(pendingMessage);
        }
    }
}