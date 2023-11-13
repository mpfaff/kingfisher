package kingfisher;

import dev.pfaff.log4truth.LogSink;
import dev.pfaff.log4truth.NamedLogger;
import kingfisher.constants.Status;
import kingfisher.requests.CallSiteHandler;
import kingfisher.responses.BuiltResponse;
import kingfisher.responses.Writer;
import kingfisher.scripting.Script;
import kingfisher.scripting.ScriptLoader;
import org.eclipse.jetty.http.HttpException;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static dev.pfaff.log4truth.StandardTags.DEBUG;
import static dev.pfaff.log4truth.StandardTags.ERROR;
import static kingfisher.Config.BIND_PORT;
import static org.eclipse.jetty.server.handler.ErrorHandler.ERROR_EXCEPTION;

public final class Main {
	public static final NamedLogger WEB_LOGGER = new NamedLogger("Web Engine");
	public static final NamedLogger SCRIPT_LOGGER = new NamedLogger("Script Engine");

	public static void main(String[] args) {
		LogSink.addFilterToGlobal((source, tags, thread) -> !source.startsWith("org.eclipse.jetty.") || !tags.contains(DEBUG));

		ScriptEngine engine = new ScriptEngine();

		new ScriptLoader(engine).launch();

		try {
			Server server = new Server();
			ServerConnector connector = new ServerConnector(server);
			connector.setPort(BIND_PORT);
			server.setDynamic(false);
			server.setConnectors(new Connector[]{connector});
			server.setHandler(new CallSiteHandler(engine.handler));
			server.setErrorHandler((request, response, callback) -> {
				String message;
				Throwable cause = (Throwable) request.getAttribute(ERROR_EXCEPTION);
				var status = Status.INTERNAL_SERVER_ERROR;
				if (cause instanceof HttpException httpException) {
					status = httpException.getCode();
					message = httpException.getReason() != null ? httpException.getReason() : Integer.toString(status);
				} else {
					message = "Internal server error";
				}
				WEB_LOGGER.log(() -> "Caught exception while handling request: " + message, cause, List.of(ERROR));
				new BuiltResponse(status,
						Map.of(),
						Writer.stringWriter("<p style=\"font-size: 4em\">" + message + "<p>"))
						.send(response, callback);
				return true;
			});
			server.start();
			server.join();
		} catch (Throwable e) {
			WEB_LOGGER.log(() -> "Server crashed", e, List.of(ERROR));
		}
	}
}
