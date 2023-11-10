package fit;

import com.google.protobuf.util.JsonFormat;
import io.netty.buffer.ByteBuf;

import java.io.IOException;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class FitNetwork {
    private final SocketChannel channel;
    private final ByteBuffer byteBuffer = ByteBuffer.allocate(16384);

    public FitNetwork(String unixSocketPath) throws IOException {
        var address = UnixDomainSocketAddress.of(unixSocketPath);
        channel = SocketChannel.open(StandardProtocolFamily.UNIX);
        channel.connect(address);
    }

    public void sendMessage(Messages.Message message) throws IOException {
        var json = JsonFormat.printer().print(message);
        byteBuffer.clear();
        byteBuffer.put(json.getBytes(StandardCharsets.UTF_8));
        byteBuffer.flip();
        while (byteBuffer.hasRemaining()) {
            channel.write(byteBuffer);
        }
    }


}
