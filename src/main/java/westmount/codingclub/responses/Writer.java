package westmount.codingclub.responses;

import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.util.Callback;

import java.io.IOException;
import java.io.OutputStream;

// shouldn't be implementable by scripts because not marked with @FunctionalInterface
// this is important because the engine is not thread safe (well, it is, but it throws if used from multiple threads
// at once), and writers are invoked on a thread pool.
public interface Writer {
	/**
	 * Writes to the provided stream. Clean up is managed by the caller.
	 */
	void write(Content.Sink sink, Callback callback) throws IOException;

	static Writer bytesWriter(byte[] content) {
		return new BytesWriter(content);
	}

	static Writer stringWriter(String s) {
		return new StringWriter(s);
	}
}
