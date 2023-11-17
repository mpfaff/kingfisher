package kingfisher.scripting;

import kingfisher.channel.ChannelScriptThread;
import kingfisher.interop.ValueUtil;

public abstract class LiveApi {
	private final ScriptEngine engine;

	protected LiveApi(ScriptEngine engine) {
		this.engine = engine;
	}

	public final Object send(String channelName, Object message) throws Exception {
		var handler = engine.channelHandlers.get(channelName);
		if (handler == null) {
			throw new Exception("No handler registered for channel '" + channelName + "'s");
		}
		return engine.runWithScript(() -> new ChannelScriptThread(engine, handler), "Script route handler", thread -> {
			try {
				var value = thread.handler.handle(message);
				return ValueUtil.resolveMaybePromise(value, thread.eventLoop);
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
		}).get();
	}
}
