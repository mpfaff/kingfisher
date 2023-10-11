package kingfisher;

import kingfisher.requests.RegexRouteHandler;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import kingfisher.requests.ProxyRequest;
import kingfisher.util.JObject;

import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static kingfisher.Config.TRACE_SCRIPT_ENGINE;

public final class InitScriptApi extends ScriptApi {
	int scriptIndex;
	private Object state;
	private boolean hasSetState;

	public InitScriptApi(ScriptEngine engine) {
		super(engine);
		this.scriptIndex = -1;
	}

	@Override
	public void setScriptIndex(int scriptIndex) {
		super.setScriptIndex(scriptIndex);
		this.scriptIndex = scriptIndex;
		state = null;
		hasSetState = false;
	}

	@Override
	public Object getState(Supplier<Object> initFunction) {
		if (hasSetState) throw new IllegalStateException("getState may be called at most once per script");
		hasSetState = true;
		return state = initFunction.get();
	}

	@Override
	public void addRoute(String method, String path, Value handler) {
		var engine = this.engine;
		var state = this.state;
		int scriptIndex = this.scriptIndex;
		int handlerId = nextHandlerId();
		engine.handlers.add(new RegexRouteHandler(method, Pattern.compile(path), (request, arguments, response, callback) -> {
			Main.SCRIPT_LOGGER.info("Submitting task to process script handler on a worker thread");
			engine.executor.submit(() -> {
				Main.SCRIPT_LOGGER.info("Processing script handler");
				var api = new HandlerInvocationScriptApi(engine, state, handlerId);
				try (var ctx = engine.createScriptContext(api, scriptIndex)) {
					engine.loadScript(ctx, scriptIndex);

					var start = System.currentTimeMillis();
					long elapsed;

					//noinspection unchecked,rawtypes
					var res = api.handler.handle(new ProxyRequest(request), new JObject((Map<String, Object>) (Map) arguments));

					if (TRACE_SCRIPT_ENGINE) {
						elapsed = System.currentTimeMillis() - start;
						Main.SCRIPT_LOGGER.info("Handled route in " + elapsed + " ms");
					}

					return res;
				} catch (Throwable e) {
					throw new RuntimeException(e);
				}
			}).get().send(response, callback);
		}));
	}
}
