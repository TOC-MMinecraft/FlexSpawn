package pine.spawnonjoin.listener;

// 用法：仅在玩家因死亡复活时覆盖目标点，不干扰末地回城或其它插件的复活流程。
import pine.spawnonjoin.service.SpawnDecisionService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

public final class PlayerRespawnListener implements Listener {

    private final SpawnDecisionService decisionService;

    public PlayerRespawnListener(SpawnDecisionService decisionService) {
        this.decisionService = decisionService;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (event.getRespawnReason() != PlayerRespawnEvent.RespawnReason.DEATH) {
            return;
        }
        event.setRespawnLocation(decisionService.resolveRespawnLocation(event.getPlayer().getUniqueId()));
    }
}
