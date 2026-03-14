package pine.flexspawn.listener;

// 用法：处理玩家首次加入或普通加入时的落点，并在进入后补齐玩家档案记录。
import pine.flexspawn.model.GroupSpawnSelectionResult;
import pine.flexspawn.service.SpawnDecisionService;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

public final class PlayerJoinSpawnListener implements Listener {

    private final Supplier<SpawnDecisionService> decisionServiceSupplier;
    private final Map<UUID, String> pendingMessages = new ConcurrentHashMap<>();

    public PlayerJoinSpawnListener(Supplier<SpawnDecisionService> decisionServiceSupplier) {
        this.decisionServiceSupplier = decisionServiceSupplier;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerSpawnLocation(PlayerSpawnLocationEvent event) {
        Optional<GroupSpawnSelectionResult> result = decisionServiceSupplier.get().resolveJoinLocation(event.getPlayer());
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
        decisionServiceSupplier.get().ensurePlayerRecord(
                event.getPlayer().getUniqueId(),
                event.getPlayer().getName()
        );

        String pendingMessage = pendingMessages.remove(event.getPlayer().getUniqueId());
        if (pendingMessage != null) {
            event.getPlayer().sendMessage(pendingMessage);
        }
    }
}
