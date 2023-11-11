package fit;

import io.netty.util.internal.StringUtil;

import java.io.IOException;
import java.util.Objects;

public class Fit {
    private static final FitNetwork FIT_NETWORK;
    public static final String MARKER_KEY;

    static {
        try {
            var socketPath = System.getenv("TO_DA_CONTAINER_SOCKET_PATH");
            if (StringUtil.isNullOrEmpty(socketPath)) {
                throw new RuntimeException("TO_DA_CONTAINER_SOCKET_PATH env variable is empty");
            }
            FIT_NETWORK = new FitNetwork(socketPath);
            MARKER_KEY = System.getenv("CRASH_KEY");
            System.out.println("Marker key of this instance: " + MARKER_KEY);
            if (StringUtil.isNullOrEmpty(MARKER_KEY)) {
                throw new RuntimeException("TO_DA_CONTAINER_SOCKET_PATH env variable is empty");
            }
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    public static void injectAction(Messages.ActionType actionType) {
        var message = Messages.Message.newBuilder()
                .setActionType(actionType)
                .build();
        try {
            System.out.printf("Sending message for action type: %s%n", actionType);
            FIT_NETWORK.sendMessage(message);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    public static void injectOnMarker(Messages.ActionType actionType, Object marker) {
        if (!Objects.equals(marker, MARKER_KEY)) {
            return;
        }

        injectAction(actionType);
    }
}
