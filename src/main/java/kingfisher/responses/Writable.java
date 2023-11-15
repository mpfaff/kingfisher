package kingfisher.responses;

import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.util.Callback;

import java.io.IOException;

/**
 * A writable value. May be passed to {@link ResponseBuilder#content(Writable)} to send to a client as the response
 * body.
 * <p>
 * <strong>Note:</strong> Do <strong>NOT</strong> implement in a script! You must use the provided implementations.
 * <p>
 * This is because the script context will be closed before the writer is ever invoked (also for thread-safety reasons).
 */
@FunctionalInterface
public interface Writable {
	/**
	 * Writes to the provided stream. Clean up is managed by the caller.
	 */
	void writeTo(Content.Sink sink, Callback callback) throws IOException;

	static Writable bytesWriter(byte[] content) {
		return new BytesWritable(content);
	}

	static Writable stringWriter(String s) {
		return new StringWritable(s);
	}
}
