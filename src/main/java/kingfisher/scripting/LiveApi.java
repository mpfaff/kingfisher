package kingfisher.scripting;

import kingfisher.channel.ScriptStatelessChannelHandler;
import kingfisher.channel.StatelessChannelScriptThread;
import kingfisher.interop.ValueUtil;

public abstract class LiveApi {
	private final ScriptThread thread;

	public final ManagementApi management;

	protected LiveApi(ScriptThread thread) {
		this.thread = thread;
		this.management = new ManagementApi(thread);
	}

	/**
	 * Sends a message to the specified {@link RegistrationApi#addStatelessChannel(String, ScriptStatelessChannelHandler) stateless channel}
	 * @param channelName
	 * @param message
	 * @return
	 * @throws Exception
	 */
	public final Object send(String channelName, Object message) throws Exception {
		var handler = thread.engine.channelHandlers.get(channelName);
		if (handler == null) {
			throw new Exception("No handler registered for channel '" + channelName + "'s");
		}
		return thread.engine.runWithScript(() -> new StatelessChannelScriptThread(thread.engine, handler), "Script route handler", thread -> {
			try {
				var value = thread.handler.handle(message);
				return ValueUtil.resolveMaybePromise(value, thread.eventLoop);
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
		}).get();
	}
}
