package kingfisher.scripting;

import kingfisher.ScriptEngine;
import kingfisher.interop.Exports;
import kingfisher.interop.JObject;
import kingfisher.requests.HttpServerRequest;
import kingfisher.requests.RegexRouteHandler;
import kingfisher.requests.RequestScriptThread;
import kingfisher.requests.ScriptRouteHandler;
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
		Exports.objectMembers(new Api()).export(scope);
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
	 * The API available to each script during the registration phase.
	 */
	public final class Api extends BaseApi {
		private Api() {}

		@Override
		public void addRoute(String method, String path, ScriptRouteHandler handler) {
			var engine = RegistrationScriptThread.this.engine;
			var script = RegistrationScriptThread.this.script;
			var staging = RegistrationScriptThread.this.staging;

			staging.checkState();

			int handlerId = nextHandlerId();
			staging.requestHandlers.add(new RegexRouteHandler(method, Pattern.compile(path), (request, arguments, response, callback) -> {
				SCRIPT_LOGGER.log(() -> "Submitting task to process script handler on a worker thread");
				engine.executor.submit(() -> {
					SCRIPT_LOGGER.log(() -> "Processing script handler");
					var thread = new RequestScriptThread(engine, script, handlerId);
					try (var ctx = engine.createExecutionScriptContext(thread)) {
						ctx.enter();
						try {
							engine.loadScript(ctx, script);

							var start = System.nanoTime();
							long elapsed;

							//noinspection unchecked,rawtypes
							var res = thread.handler.handle(new HttpServerRequest(thread, request), JObject.wrap((Map<String, Object>) (Map) arguments));

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
			}));
		}
	}
}
