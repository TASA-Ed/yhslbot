package top.tasaed.slbot.util;

import lombok.Getter;

import java.util.Collections;
import java.util.List;

@Getter
public class PortCheckResult {
    private final List<Integer> existingPorts;
    private final List<Integer> missingPorts;

    public PortCheckResult(List<Integer> existing, List<Integer> missing) {
        this.existingPorts = Collections.unmodifiableList(existing);
        this.missingPorts = Collections.unmodifiableList(missing);
    }

    public boolean allExist() {
        return missingPorts.isEmpty();
    }

    @Override
    public String toString() {
        return "存在的端口: " + existingPorts + "\n缺失的端口: " + missingPorts;
    }
}