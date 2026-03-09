package pine.spawnonjoin.listener;

// 用法：处理玩家首次加入或普通加入时的落点，并在玩家成功进入后补齐档案记录。
import pine.spawnonjoin.service.SpawnDecisionService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

public final class PlayerJoinSpawnListener implements Listener {

    private final SpawnDecisionService decisionService;

    public PlayerJoinSpawnListener(SpawnDecisionService decisionService) {
        this.decisionService = decisionService;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerSpawnLocation(PlayerSpawnLocationEvent event) {
        event.setSpawnLocation(decisionService.resolveJoinLocation(event.getPlayer().getUniqueId()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        decisionService.ensurePlayerRecord(
                event.getPlayer().getUniqueId(),
                event.getPlayer().getName()
        );
    }
}
