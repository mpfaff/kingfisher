package westmount.codingclub.responses;

import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.util.Callback;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Objects;

public record BytesWriter(byte[] bytes) implements Writer {
	public BytesWriter {
		Objects.requireNonNull(bytes);
	}

	@Override
	public void write(Content.Sink sink, Callback callback) {
		sink.write(true, ByteBuffer.wrap(bytes), callback);
	}
}
