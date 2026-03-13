package pine.flexspawn.model;

// 用法：枚举插件会接管的公共出生场景，统一配置路径与决策分支。
public enum SpawnScenario {

    FIRST_JOIN("first-join"),
    JOIN_DEFAULT("join-default"),
    DEATH_RESPAWN("death-respawn");

    private final String configPath;

    SpawnScenario(String configPath) {
        this.configPath = configPath;
    }

    public String configPath() {
        return configPath;
    }
}