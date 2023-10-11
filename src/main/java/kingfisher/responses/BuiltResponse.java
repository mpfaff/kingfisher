package kingfisher.responses;

import kingfisher.Main;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import java.util.Map;
import java.util.Objects;

public record BuiltResponse(int status, Map<String, String> headers, Writer writer) {
	public BuiltResponse {
		Objects.requireNonNull(headers);
	}

	public void send(Response response, Callback callback) {
		try {
			response.setStatus(status);
			headers.forEach(response.getHeaders()::add);
			Main.WEB_LOGGER.info("Transmitted status and content type");
			if (writer != null) {
				writer.write(response, callback);
				Main.WEB_LOGGER.info("Transmitted content");
			}
		} catch (Throwable e) {
			Main.WEB_LOGGER.error("Caught exception while transmitting response", e);
		}
	}
}
