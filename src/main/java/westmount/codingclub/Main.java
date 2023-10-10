package westmount.codingclub;

import org.eclipse.jetty.http.HttpException;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import westmount.codingclub.constants.Status;
import westmount.codingclub.requests.CascadingHandler;
import westmount.codingclub.responses.BuiltResponse;
import westmount.codingclub.responses.Writer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

import static org.eclipse.jetty.server.handler.ErrorHandler.ERROR_EXCEPTION;
import static org.eclipse.jetty.server.handler.ErrorHandler.ERROR_MESSAGE;

public final class Main {
	public static final Logger WEB_LOGGER = LoggerFactory.getLogger("Web Engine");
	public static final Logger SCRIPT_LOGGER = LoggerFactory.getLogger("Script Engine");

	private static String envVar(String name, String defaultValue) {
		return Objects.requireNonNullElse(System.getenv(name), defaultValue);
	}

	public static final int BIND_PORT = Integer.parseInt(envVar("BIND_PORT", "8080"));
	public static final Path SCRIPTS_DIR = Path.of(envVar("SCRIPTS_DIR", "scripts"));
	public static final Path TEMPLATES_DIR = Path.of(envVar("TEMPLATES_DIR", "templates"));

	public static void main(String[] args) {
		ScriptEngine engine;
		try {
			engine = new ScriptEngine();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		for (int i = 0; i < engine.scriptCount(); i++) {
			try (var ctx = engine.loadScripts(new InitScriptApi(engine, i), i)) {
			}
		}

		try {
			Server server = new Server();
			ServerConnector connector = new ServerConnector(server);
			connector.setPort(BIND_PORT);
			server.setDynamic(false);
			server.setConnectors(new Connector[]{connector});
			server.setHandler(new CascadingHandler(engine.handlers));
			server.setErrorHandler((request, response, callback) -> {
				String message = "Internal server error";
				Throwable cause = (Throwable)request.getAttribute(ERROR_EXCEPTION);
				var status = Status.INTERNAL_SERVER_ERROR;
				if (cause instanceof HttpException httpException)
				{
					status = httpException.getCode();
					message = httpException.getReason();
					if (message == null) {
						message = Integer.toString(status);
					}
				}
				WEB_LOGGER.error("Caught exception while handling request: " + message, cause);
				new BuiltResponse(status,
						Map.of(),
						Writer.stringWriter("<p style=\"font-size: 4em\">" + message + "<p>"))
						.send(response, callback);
				return true;
			});
			server.start();
			server.join();
		} catch (Throwable e) {
			WEB_LOGGER.error("Server crashed", e);
		}
	}
}
