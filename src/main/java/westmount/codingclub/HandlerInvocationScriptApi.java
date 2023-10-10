package westmount.codingclub;

import org.graalvm.polyglot.Value;
import westmount.codingclub.requests.ScriptRequestHandler;

import java.util.function.Supplier;

public final class HandlerInvocationScriptApi extends ScriptApi {
	private final Object state;
	private final int targetHandler;
	public ScriptRequestHandler handler;

	public HandlerInvocationScriptApi(ScriptEngine engine, Object state, int targetHandler) {
		super(engine);
		this.state = state;
		this.targetHandler = targetHandler;
	}

	@Override
	public Object getState(Supplier<Object> initFunction) {
		return state;
	}

	@Override
	public void addRoute(String method, String path, Value handler) {
		if (nextHandlerId() == targetHandler) {
			// TODO: do we need to pin?
			this.handler = handler.as(ScriptRequestHandler.class);
		}
	}
}
