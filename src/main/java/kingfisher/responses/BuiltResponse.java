package kingfisher.responses;

import kingfisher.Main;
import kingfisher.ScriptEngine;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static dev.pfaff.log4truth.StandardTags.ERROR;

public record BuiltResponse(int status, Map<String, String> headers, Writer writer) {
	public BuiltResponse {
		Objects.requireNonNull(headers);
	}

	public static BuiltResponse error(ScriptEngine engine, int status, String message) {
		var s = new StringWriter();
		try {
			engine.pebble.getTemplate("error.html")
					.evaluate(s, Map.of("message", message));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return new ResponseBuilder()
				.status(status)
				.content(s.toString())
				.finish();
	}

	public void send(Response response, Callback callback) {
		try {
			response.setStatus(status);
			headers.forEach(response.getHeaders()::add);
			Main.WEB_LOGGER.log(() -> "Transmitted status and content type");
			if (writer != null) {
				writer.write(response, callback);
				Main.WEB_LOGGER.log(() -> "Transmitted content");
			}
		} catch (Throwable e) {
			Main.WEB_LOGGER.log(() -> "Caught exception while transmitting response", e, List.of(ERROR));
		}
	}
}
