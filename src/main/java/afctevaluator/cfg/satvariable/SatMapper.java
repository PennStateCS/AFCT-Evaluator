package afctevaluator.cfg.satvariable;

import java.util.HashMap;
import java.util.List;

public class SatMapper {

    HashMap<SatVariable, Integer> variableMap;

    public SatMapper() {
        variableMap = new HashMap<>(100_000, 0.5f);
    }

    public Integer encodePositive(SatVariable s) {
        return variableMap.computeIfAbsent(s, ignored -> variableMap.size());
    }

    public Integer encodeNegative(SatVariable s) {
        return -variableMap.computeIfAbsent(s, ignored -> variableMap.size());
    }

    public int maxVar() {
        return variableMap.size();
    }
}
