package kingfisher.responses;

import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.util.Callback;

import java.nio.ByteBuffer;
import java.util.Objects;

public record BytesWritable(byte[] bytes) implements Writable {
	public BytesWritable {
		Objects.requireNonNull(bytes);
	}

	@Override
	public void writeTo(Content.Sink sink, Callback callback) {
		sink.write(true, ByteBuffer.wrap(bytes), callback);
	}
}
