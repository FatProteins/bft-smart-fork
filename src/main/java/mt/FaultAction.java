package mt;

public interface FaultAction {
    void perform() throws Exception;

    String getName();

    ActionType getType();
}
