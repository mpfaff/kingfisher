package kingfisher;

import org.eclipse.jetty.http.HttpException;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import kingfisher.constants.Status;
import kingfisher.requests.CascadingHandler;
import kingfisher.responses.BuiltResponse;
import kingfisher.responses.Writer;

import java.io.IOException;
import java.util.Map;

import static org.eclipse.jetty.server.handler.ErrorHandler.ERROR_EXCEPTION;
import static kingfisher.Config.BIND_PORT;

public final class Main {
	public static final Logger WEB_LOGGER = LoggerFactory.getLogger("Web Engine");
	public static final Logger SCRIPT_LOGGER = LoggerFactory.getLogger("Script Engine");

	public static void main(String[] args) {
		ScriptEngine engine;
		try {
			engine = new ScriptEngine();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		var api = new InitScriptApi(engine);
		try (var ctx = engine.createScriptContext(api)) {
			for (int i = 0; i < engine.scriptCount(); i++) {
				api.setScriptIndex(i);
				engine.loadScript(ctx, i);
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
