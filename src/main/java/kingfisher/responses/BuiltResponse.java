package kingfisher.responses;

import kingfisher.Main;
import kingfisher.scripting.ScriptEngine;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static dev.pfaff.log4truth.StandardTags.DEBUG;
import static dev.pfaff.log4truth.StandardTags.ERROR;
import static kingfisher.Main.WEB_LOGGER;

public record BuiltResponse(int status, Map<String, String> headers, Writable writable) {
	public BuiltResponse {
		Objects.requireNonNull(headers);
	}

	public static BuiltResponse error(ScriptEngine engine, int status, String message) {
		return new ErrorResponseBuilder(engine, status, message).finish();
	}

	public void send(Response response, Callback callback) {
		try {
			response.setStatus(status);
			headers.forEach(response.getHeaders()::add);
			WEB_LOGGER.log(() -> "Transmitted status and content type", List.of(DEBUG));
			if (writable != null) {
				writable.writeTo(response, callback);
				WEB_LOGGER.log(() -> "Transmitted content", List.of(DEBUG));
			} else {
				callback.succeeded();
//				response.write(true, ByteBuffer.allocate(0), callback);
			}
		} catch (Throwable e) {
			WEB_LOGGER.log(() -> "Caught exception while transmitting response", e, List.of(ERROR));
		}
	}
}
