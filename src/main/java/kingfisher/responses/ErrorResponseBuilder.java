package kingfisher.responses;

import dev.pfaff.log4truth.Logger;
import kingfisher.scripting.ScriptEngine;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dev.pfaff.log4truth.StandardTags.ERROR;
import static kingfisher.Main.WEB_LOGGER;

public final class ErrorResponseBuilder {
	private final ScriptEngine engine;

	private final int status;
	private final String message;
	private final List<String> diagnostics = new ArrayList<>();

	public ErrorResponseBuilder(ScriptEngine engine, int status, String message) {
		this.engine = engine;
		this.status = status;
		this.message = message;
	}

	public ErrorResponseBuilder diagnostic(String message) {
		this.diagnostics.add(message);
		return this;
	}

	public BuiltResponse finish() {
		var s = new StringWriter();
		boolean ok = true;
		try {
			engine.pebble.getTemplate("error.html")
					.evaluate(s, Map.of("message", message, "diagnostics", diagnostics));
		} catch (IOException e) {
			WEB_LOGGER.log(() -> "Unable to render error page", e, List.of(ERROR));
			s.getBuffer().setLength(0);
			s.write("<RENDER ERROR>");
			ok = false;
		}
		var builder = new ResponseBuilder()
				.status(status)
				.content(s.toString());
		if (ok) builder.html();
		return builder.finish();
	}
}
