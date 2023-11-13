package kingfisher;

import kingfisher.requests.RegexRouteHandler;
import kingfisher.scripting.Script;
import org.graalvm.polyglot.Value;
import kingfisher.requests.ProxyRequest;
import kingfisher.util.JObject;

import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static kingfisher.Config.TRACE_SCRIPT_ENGINE;

public final class InitScriptApi extends ScriptApi {
	private Script script;
	private Object state;
	private boolean hasSetState;
	private final InitStagingArea staging;

	public InitScriptApi(ScriptEngine engine, InitStagingArea staging) {
		super(engine);
		this.staging = staging;
	}

	public void setScript(Script script) {
		resetHandlerId();
		this.script = script;
		state = null;
		hasSetState = false;
	}

	@Override
	public Object getState(Supplier<Object> initFunction) {
		staging.checkState();

		if (hasSetState) throw new IllegalStateException("getState may be called at most once per script");
		hasSetState = true;
		return state = initFunction.get();
	}

	@Override
	public void addRoute(String method, String path, Value handler) {
		staging.checkState();

		var engine = this.engine;
		var state = this.state;
		var script = this.script;
		int handlerId = nextHandlerId();
		staging.handlers.add(new RegexRouteHandler(method, Pattern.compile(path), (request, arguments, response, callback) -> {
			Main.SCRIPT_LOGGER.log(() -> "Submitting task to process script handler on a worker thread");
			engine.executor.submit(() -> {
				Main.SCRIPT_LOGGER.log(() -> "Processing script handler");
				var api = new HandlerInvocationScriptApi(engine, state, handlerId);
				try (var ctx = engine.createScriptContext(api, script)) {
					engine.loadScript(ctx, script);

					var start = System.currentTimeMillis();
					long elapsed;

					//noinspection unchecked,rawtypes
					var res = api.handler.handle(new ProxyRequest(request), new JObject((Map<String, Object>) (Map) arguments));

					if (TRACE_SCRIPT_ENGINE) {
						elapsed = System.currentTimeMillis() - start;
						Main.SCRIPT_LOGGER.log(() -> "Handled route in " + elapsed + " ms");
					}

					return res;
				} catch (Throwable e) {
					throw new RuntimeException(e);
				}
			}).get().send(response, callback);
		}));
	}
}
