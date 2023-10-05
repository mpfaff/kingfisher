package westmount.codingclub.responses;

import name.martingeisse.grumpyrest.response.Response;
import name.martingeisse.grumpyrest.response.ResponseTransmitter;

import java.util.Map;
import java.util.Objects;

import static westmount.codingclub.Main.WEB_LOGGER;

public record BuiltResponse(int status, Map<String, String> headers, Writer writer) implements Response {
	public BuiltResponse {
		Objects.requireNonNull(headers);
	}

	@Override
	public void transmit(ResponseTransmitter transmitter) {
		try {
			transmitter.setStatus(status);
			headers.forEach(transmitter::addCustomHeader);
			WEB_LOGGER.info("Transmitted status and content type");
			if (writer != null) {
				try (var stream = transmitter.getOutputStream()) {
					writer.write(stream);
				}
				WEB_LOGGER.info("Transmitted content");
			}
		} catch (Throwable e) {
			WEB_LOGGER.error("Caught exception while transmitting response", e);
		}
	}
}
