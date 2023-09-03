package mt;

import java.security.SecureRandom;
import java.util.Map;

public class ActionPicker {
    private final double[] cumProbabilities;
    private final Map<ActionType, FaultAction> actions;
    private final Map<Integer, ActionType> actionMapping;
    private final FaultConfig faultConfig;
    private final SecureRandom random = new SecureRandom();

    public ActionPicker(FaultConfig faultConfig) {
        var probabilities = new double[]{
                faultConfig.getActions().getNoop().getProbability(),
                faultConfig.getActions().getHalt().getProbability(),
                faultConfig.getActions().getPause().getProbability(),
                faultConfig.getActions().getStop().getProbability(),
                faultConfig.getActions().getResendLastMessage().getProbability()
        };
        cumProbabilities = new double[probabilities.length];
        var sum = 0.0d;
        for (var i = 0; i < probabilities.length; i++) {

            // find sum
            sum += probabilities[i];

            // replace
            cumProbabilities[i] = sum;
        }

        actions = Map.of(
                ActionType.NOOP, new NoopAction(),
                ActionType.HALT, new HaltAction(faultConfig),
                ActionType.RESEND_LAST_MESSAGE, new ResendLastMessageAction(faultConfig)
        );
        actionMapping = Map.of(
                0, ActionType.NOOP,
                1, ActionType.HALT,
                2, ActionType.PAUSE,
                3, ActionType.STOP,
                4, ActionType.RESEND_LAST_MESSAGE
        );
        this.faultConfig = faultConfig;
    }

    public ActionType determineAction() {
        var randomValue = random.nextDouble();
        var val = randomValue * cumProbabilities[cumProbabilities.length - 1];
        var actionIdx = FaultUtils.binarySearch(cumProbabilities.length, idx -> cumProbabilities[idx] > val);
        return actionMapping.get(actionIdx);
    }

    public FaultAction getAction(ActionType actionType) {
        return actions.get(actionType);
    }
}
