package westmount.codingclub;

import org.graalvm.polyglot.Value;
import westmount.codingclub.requests.ProxyRequest;
import westmount.codingclub.requests.RegexRouteHandler;
import westmount.codingclub.util.JObject;

import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static westmount.codingclub.Main.SCRIPT_LOGGER;

public final class InitScriptApi extends ScriptApi {
	private final int scriptIndex;
	private Object state;
	private boolean hasSetState;

	public InitScriptApi(ScriptEngine engine, int scriptIndex) {
		super(engine);
		this.scriptIndex = scriptIndex;
	}

	@Override
	public Object getState(Supplier<Object> initFunction) {
		if (hasSetState) throw new IllegalStateException("getState may be called at most once per script");
		hasSetState = true;
		return state = initFunction.get();
	}

	@Override
	public void addRoute(String method, String path, Value handler) {
		var state = this.state;
		int handlerId = nextHandlerId();
		engine.handlers.add(new RegexRouteHandler(method, Pattern.compile(path), (request, arguments, response, callback) -> {
			SCRIPT_LOGGER.info("Forwarding handler for request on " + Thread.currentThread() + " to " + engine.executor);
			engine.executor.submit(() -> {
				var api = new HandlerInvocationScriptApi(engine, state, handlerId);
				try (var ctx = engine.loadScripts(api, scriptIndex)) {
					//noinspection unchecked,rawtypes
					return api.handler.handle(new ProxyRequest(request), new JObject((Map<String, Object>) (Map) arguments));
				} catch (Throwable e) {
					throw new RuntimeException(e);
				}
			}).get().send(response, callback);
		}));
	}
}
