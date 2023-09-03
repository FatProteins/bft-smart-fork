package mt;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Slf4j
public class Interrupt {
    private final SocketChannel socketChannel;
    private final AtomicReference<ActionPicker> actionPicker;
    private final boolean enabled;
    private final Lock daDataLock = new ReentrantLock();
    private final ReadWriteLock daInterruptLock = new ReentrantReadWriteLock();
    private final ByteBuffer sendBuffer = ByteBuffer.wrap(new byte[10 * 4096]);
    private final ByteBuffer respBuffer = ByteBuffer.wrap(new byte[10 * 4096]);

    public Interrupt() {
        socketChannel = null;
        actionPicker = null;
        enabled = false;
    }

    public Interrupt(ActionPicker actionPicker, String toDaSocketPath) throws IOException {
//        var toDaSocketPath = System.getenv("TO_DA_CONTAINER_SOCKET_PATH");
//        if (StringUtils.isBlank(toDaSocketPath)) {
//            throw new RuntimeException("To-DA Socket path env variable is empty");
//        }

        var toDaUnixAddr = UnixDomainSocketAddress.of(toDaSocketPath);
        socketChannel = SocketChannel.open(StandardProtocolFamily.UNIX);
        socketChannel.configureBlocking(true);
        socketChannel.connect(toDaUnixAddr);

        this.actionPicker = new AtomicReference<>(actionPicker);
        enabled = true;
    }

    public void pickAction() {
        if (!enabled) {
            return;
        }

        var actionType = actionPicker.get().determineAction();
        if (actionType == ActionType.NOOP) {
            try {
                daDataLock.lock();
                // save last message data: index etc.
            } finally {
                daDataLock.unlock();
            }
        } else if (actionType == ActionType.RESEND_LAST_MESSAGE) {
            // use last message data to resend
            try {
                daDataLock.lock();
            } finally {
                daDataLock.unlock();
            }
        }
    }

    public void setActionPicker(ActionPicker actionPicker) {
        this.actionPicker.set(actionPicker);
    }

    public void daInterrupt(ActionType actionType) throws IOException {
        try {
            daInterruptLock.writeLock().lock();
            sendBuffer.clear();
            // Marshal message and fill buffer
            socketChannel.write(sendBuffer);

            respBuffer.clear();
            var bytesRead = socketChannel.read(respBuffer);
            // Unmarshal message
        } finally {
            daInterruptLock.writeLock().unlock();
        }
    }
}
