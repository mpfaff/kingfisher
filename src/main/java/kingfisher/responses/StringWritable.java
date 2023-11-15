package kingfisher.responses;

import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.util.Callback;

import java.util.Objects;

public record StringWritable(String string) implements Writable {
	public StringWritable {
		Objects.requireNonNull(string);
	}

	@Override
	public void writeTo(Content.Sink sink, Callback callback) {
		Content.Sink.write(sink, true, string, callback);
	}
}
