package fit;

import com.google.protobuf.util.JsonFormat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class FitNetwork {
    private static final JsonFormat.Printer PRINTER = JsonFormat.printer()
            .omittingInsignificantWhitespace();
    private final SocketChannel channel;
    private final ByteBuffer byteBuffer = ByteBuffer.allocate(16384);

    public FitNetwork(String unixSocketPath) throws IOException {
        var address = UnixDomainSocketAddress.of(unixSocketPath);
        channel = SocketChannel.open(StandardProtocolFamily.UNIX);
        channel.connect(address);
        System.out.println("Connected to address %s with socket path %s - %b".formatted(address.toString(), unixSocketPath, channel.finishConnect()));
    }

    public void sendMessage(Messages.Message message) throws IOException {
//        var json = PRINTER.print(message) + "\n";
//        System.out.printf("Sending json: %s", json);
        System.out.printf("Triggering %s action%n", message.getActionType());

        var stream = new ByteArrayOutputStream();
        message.writeDelimitedTo(stream);
        var buffer = ByteBuffer.wrap(stream.toByteArray());
        while (buffer.hasRemaining()) {
            channel.write(buffer);
        }
    }


}
