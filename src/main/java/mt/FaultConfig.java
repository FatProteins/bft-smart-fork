package mt;

import lombok.Value;

@Value
public class FaultConfig {
    String unixToDaDomainSocketPath;
    String unixFromDaDomainSocketPath;
    Boolean faultsEnabled;
    Actions actions;

    @Value
    public static class Actions {
        Noop noop;
        Halt halt;
        Pause pause;
        Stop stop;
        ResendLastMessage resendLastMessage;
    }

    @Value
    public static class Noop {
        Double probability;
    }

    @Value
    public static class Halt {
        Double probability;
        Integer maxDuration;
    }

    @Value
    public static class Pause {
        Double probability;
        Integer maxDuration;
        String pauseCommand;
        String continueCommand;
    }

    @Value
    public static class Stop {
        Double probability;
        Integer maxDuration;
        String stopCommand;
        String restartCommand;
    }

    @Value
    public static class ResendLastMessage {
        Double probability;
    }
}
