package fit;

import java.io.IOException;

public class Fit {
    private static final FitNetwork fitNetwork;

    static {
        try {
            var socketPath = System.getenv("TO_DA_CONTAINER_SOCKET_PATH");
            fitNetwork = new FitNetwork(socketPath);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    public static void injectAction(Messages.ActionType actionType) {
        var message = Messages.Message.newBuilder()
                .setActionType(actionType)
                .build();
        try {
            fitNetwork.sendMessage(message);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }
}
