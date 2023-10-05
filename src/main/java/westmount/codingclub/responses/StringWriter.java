package westmount.codingclub.responses;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Objects;

public record StringWriter(String string) implements Writer {
	public StringWriter {
		Objects.requireNonNull(string);
	}

	@Override
	public void write(OutputStream os) throws IOException {
		var ow = new OutputStreamWriter(os);
		ow.write(string);
		ow.flush();
	}
}
