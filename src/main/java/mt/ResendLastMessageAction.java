package mt;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class ResendLastMessageAction implements FaultAction {
    private final FaultConfig faultConfig;
    private final FaultCommand stopCommand;
    private final FaultCommand restartCommand;

    public ResendLastMessageAction(FaultConfig faultConfig) {
        this.faultConfig = faultConfig;
        this.stopCommand = FaultUtils.splitCommand(faultConfig.getActions().getPause().getPauseCommand());
        this.restartCommand = FaultUtils.splitCommand(faultConfig.getActions().getPause().getContinueCommand());
    }

    @Override
    public void perform() throws IOException, InterruptedException {
        var stopConfig = faultConfig.getActions().getStop();
        var stopProcess = new ProcessBuilder(stopCommand.getCommand())
                .start();

        var stopCmdCode = stopProcess.waitFor();
        if (stopCmdCode != 0) {
            log.info("Failed to execute stop command, exit code '{}'", stopCmdCode);
            return;
        }

        Thread.sleep(stopConfig.getMaxDuration());

        var restartProcess = new ProcessBuilder(restartCommand.getCommand())
                .start();
        var restartCmdCode = restartProcess.waitFor();
        if (restartCmdCode != 0) {
            log.info("Failed to execute restart command, exit code '{}'", restartCmdCode);
        }
    }

    @Override
    public String getName() {
        return "Stop";
    }

    @Override
    public ActionType getType() {
        return ActionType.STOP;
    }
}
