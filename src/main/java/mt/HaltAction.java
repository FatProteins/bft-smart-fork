package mt;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HaltAction implements FaultAction {
    private final FaultConfig faultConfig;

    public HaltAction(FaultConfig faultConfig) {
        this.faultConfig = faultConfig;
    }

    @Override
    public void perform() throws InterruptedException {
        Thread.sleep(faultConfig.getActions().getHalt().getMaxDuration());
    }

    @Override
    public String getName() {
        return "Halt";
    }

    @Override
    public ActionType getType() {
        return ActionType.HALT;
    }
}
