package mt;

public class NoopAction implements FaultAction {
    @Override
    public void perform() {
        // Do nothing
    }

    @Override
    public String getName() {
        return "Noop";
    }

    @Override
    public ActionType getType() {
        return ActionType.NOOP;
    }
}
