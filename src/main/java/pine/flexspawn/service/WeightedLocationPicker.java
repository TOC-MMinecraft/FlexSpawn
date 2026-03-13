package pine.flexspawn.service;

// 用法：按照权重从候选坐标点中随机选择一个结果。
import pine.flexspawn.model.WeightedLocationEntry;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class WeightedLocationPicker {

    public WeightedLocationEntry pick(List<WeightedLocationEntry> candidates) {
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("candidates 不能为空。");
        }

        long totalWeight = 0L;
        for (WeightedLocationEntry candidate : candidates) {
            totalWeight += candidate.weight();
        }

        long target = ThreadLocalRandom.current().nextLong(totalWeight) + 1L;
        long currentWeight = 0L;
        for (WeightedLocationEntry candidate : candidates) {
            currentWeight += candidate.weight();
            if (target <= currentWeight) {
                return candidate;
            }
        }

        return candidates.get(candidates.size() - 1);
    }
}
