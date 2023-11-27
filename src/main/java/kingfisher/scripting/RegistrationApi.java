package kingfisher.scripting;

import kingfisher.channel.ScriptStatelessChannelHandler;
import kingfisher.constants.Method;
import kingfisher.requests.ScriptRouteHandler;

/**
 * All methods for scripts to register to handle events. The implementation of this API is context-sensitive.
 * Specifically, registration only works during the registration phase. In other phases, the script <em>may</em>
 * be re-evaluated, but registration functions will be used for selecting the handler to use, not actually
 * registering a handler.
 */
public sealed abstract class RegistrationApi permits RegistrationScriptThread.RegistrationApi, LiveRegistrationApi {
	protected RegistrationApi() {
	}

	public abstract void requestPermission(String permission) throws PermissionException;

	/**
	 * Registers a handler for the specified route.
	 *
	 * @param method  the HTTP method to match. See {@link Method}.
	 * @param path    the url path pattern to match. This is a {@link java.util.regex.Pattern regular expression}.
	 * @param handler the handler for the route.
	 */
	public abstract void addRoute(String method, String path, ScriptRouteHandler handler);

	/**
	 * Registers a new stateless channel. There may only be a single handler registered for a given channel across all
	 * scripts. An error will be logged if a second handler is registered, but no exception will be thrown.
	 *
	 * @param name the name of the channel.
	 * @param handler the function to handle messages sent on the channel. You must not send any non-sharable data.
	 */
	public abstract void addStatelessChannel(String name, ScriptStatelessChannelHandler handler);
}
