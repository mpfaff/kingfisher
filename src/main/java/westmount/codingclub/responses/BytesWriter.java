package westmount.codingclub.responses;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

public record BytesWriter(byte[] bytes) implements Writer {
	public BytesWriter {
		Objects.requireNonNull(bytes);
	}

	@Override
	public void write(OutputStream os) throws IOException {
		os.write(bytes);
	}
}
