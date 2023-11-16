package kingfisher.scripting;

import kingfisher.interop.Exports;
import kingfisher.interop.JObject;
import kingfisher.requests.*;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.graalvm.polyglot.Value;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static kingfisher.Main.SCRIPT_LOGGER;
import static kingfisher.util.Timing.formatTime;

/**
 * The registration phase loads each script and evaluates them so that they can register their routes with the engine.
 */
public final class RegistrationScriptThread extends ScriptThread {
	private Script script;
	private final Registrar staging;

	public RegistrationScriptThread(ScriptEngine engine, Registrar staging) {
		super(engine);
		this.staging = staging;
	}

	@Override
	public void exportApi(String lang, Value scope) {
		super.exportApi(lang, scope);
		Exports.objectMembers(new RegistrationApi()).export(scope);
	}

	@Override
	public Script script() {
		return script;
	}

	public void setScript(Script script) {
		resetHandlerId();
		this.script = script;
	}

	/**
	 * Refer to {@link ScriptThread.RegistrationApi}.
	 */
	public final class RegistrationApi extends ScriptThread.RegistrationApi {
		private RegistrationApi() {}

		@Override
		public void addRoute(String method, String path, ScriptRouteHandler handler) {
			var engine = RegistrationScriptThread.this.engine;
			var script = RegistrationScriptThread.this.script;
			var staging = RegistrationScriptThread.this.staging;

			staging.checkState();

			int handlerId = nextHandlerId();
			staging.requestHandlers.add(new RegexRouteHandler(method,
					Pattern.compile(path),
					new WrappedScriptRouteHandler(engine, script, handlerId)));
		}

		private static final class WrappedScriptRouteHandler implements RouteHandler {
			private final ScriptEngine engine;
			private final Script script;
			private final int handlerId;

			public WrappedScriptRouteHandler(ScriptEngine engine, Script script, int handlerId) {
				this.engine = engine;
				this.script = script;
				this.handlerId = handlerId;
			}

			@Override
			public String toString() {
				return "Handler[" + "script=" + script +
						", " + "handlerId=" + handlerId +
						']';
			}

			@Override
			public void handle(Request request, Map<String, String> arguments, Response response, Callback callback) throws Exception {
				engine.executor.submit(() -> {
					var thread = new RequestScriptThread(engine, script, handlerId);
					try (var ctx = engine.createExecutionScriptContext(thread)) {
						ctx.enter();
						try {
							engine.loadScript(ctx, script);

							var start = System.nanoTime();
							long elapsed;

							//noinspection unchecked,rawtypes
							var res = thread.handler.handle(new HttpServerRequest(thread, request),
									JObject.wrap((Map<String, Object>) (Map) arguments));

							if (res == null) {
								throw new NullPointerException("Script returned null response object");
							}

							elapsed = System.nanoTime() - start;
							SCRIPT_LOGGER.log(() -> "Script route handler took " + formatTime(elapsed), List.of("TIMING"));

							return res;
						} finally {
							ctx.leave();
						}
					}
				}).get().send(response, callback);
			}
		}
	}
}
