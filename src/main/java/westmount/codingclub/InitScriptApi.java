package westmount.codingclub;

import name.martingeisse.grumpyrest.request.HttpMethod;
import org.graalvm.polyglot.Value;
import westmount.codingclub.constants.Status;
import westmount.codingclub.requests.ProxyRequest;
import westmount.codingclub.responses.ResponseBuilder;

import java.util.function.Supplier;

import static westmount.codingclub.Main.SCRIPT_LOGGER;

public final class InitScriptApi extends ScriptApi {
	private Object state;
	private boolean hasSetState;

	public InitScriptApi(ScriptEngine engine) {
		super(engine);
	}

	@Override
	public void startNewScript(String name) {
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
	public void addRoute(HttpMethod method, String path, Value handler) {
		var state = this.state;
		engine.api.addRoute(method, path, req -> {
			SCRIPT_LOGGER.info("Forwarding handler for request on " + Thread.currentThread() + " to " + engine.executor);
			return engine.executor.submit(() -> {
				var api = new HandlerScriptApi(engine, state, method, path);
				try (var ctx = engine.loadScripts(api)) {
					return api.handler.handle(new ProxyRequest(req));
				} catch (Throwable e) {
					SCRIPT_LOGGER.error("Caught exception while handling request", e);
					return new ResponseBuilder()
							.status(Status.INTERNAL_SERVER_ERROR)
							.content("<p style=\"font-size: 4em\">Internal server error<p>")
							.html()
							.finish();
				}
			}).get();
		});
	}
}
