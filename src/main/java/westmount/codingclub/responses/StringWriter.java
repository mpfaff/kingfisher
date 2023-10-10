package westmount.codingclub.responses;

import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.util.Callback;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Objects;

public record StringWriter(String string) implements Writer {
	public StringWriter {
		Objects.requireNonNull(string);
	}

	@Override
	public void write(Content.Sink sink, Callback callback) {
		Content.Sink.write(sink, true, string, callback);
	}
}
