package mt;

import lombok.Getter;

@Getter
public enum ActionType {
    NOOP(0),
    HALT(1),
    PAUSE(2),
    STOP(3),
    RESEND_LAST_MESSAGE(4);

    private final Integer number;

    ActionType(Integer number) {
        this.number = number;
    }
}
