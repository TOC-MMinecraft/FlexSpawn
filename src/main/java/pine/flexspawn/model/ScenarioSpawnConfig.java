package pine.flexspawn.model;

// 用法：统一封装单个公共出生场景的启用状态与坐标组引用。
public record ScenarioSpawnConfig(
        boolean enabled,
        GroupSpawnReference reference
) {

    public ScenarioSpawnConfig {
        if (enabled && reference == null) {
            throw new IllegalArgumentException("启用的出生场景必须提供坐标组引用。");
        }
    }

    public static ScenarioSpawnConfig disabled() {
        return new ScenarioSpawnConfig(false, null);
    }

    public GroupSpawnReference requiredReference() {
        if (reference == null) {
            throw new IllegalStateException("当前出生场景未配置可用引用。");
        }
        return reference;
    }
}