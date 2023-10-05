package westmount.codingclub;

import name.martingeisse.grumpyrest.request.HttpMethod;
import org.graalvm.polyglot.Value;
import westmount.codingclub.requests.RequestHandler;

import java.util.function.Supplier;

public final class HandlerScriptApi extends ScriptApi {
	private final Object state;
	private final HttpMethod targetMethod;
	private final String targetPath;
	public RequestHandler handler;

	public HandlerScriptApi(ScriptEngine engine, Object state, HttpMethod method, String path) {
		super(engine);
		this.state = state;
		this.targetMethod = method;
		this.targetPath = path;
	}

	@Override
	public void startNewScript(String name) {
	}

	@Override
	public Object getState(Supplier<Object> initFunction) {
		return state;
	}

	@Override
	public void addRoute(HttpMethod method, String path, Value handler) {
		if (method == targetMethod && targetPath.equals(path)) {
			// TODO: do we need to pin?
			this.handler = handler.as(RequestHandler.class);
		}
	}
}
