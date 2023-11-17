package kingfisher.scripting;

import kingfisher.channel.ScriptChannelHandler;
import kingfisher.constants.Method;
import kingfisher.requests.ScriptRouteHandler;

/**
 * All methods for scripts to register to handle events. The implementation of this API is context-sensitive.
 * Specifically, registration only works during the registration phase. In other phases, the script <em>may</em>
 * be re-evaluated, but registration functions will be used for selecting the handler to use, not actually
 * registering a handler.
 */
public abstract class RegistrationApi {
	protected RegistrationApi() {
	}

	/**
	 * Registers a handler for the specified route.
	 *
	 * @param method  the HTTP method to match. See {@link Method}.
	 * @param path    the url path pattern to match. This is a {@link java.util.regex.Pattern regular expression}.
	 * @param handler the handler for the route.
	 */
	public abstract void addRoute(String method, String path, ScriptRouteHandler handler);

	/**
	 * Registers a handler for the specified channel.
	 *
	 * @param name the name of the channel.
	 */
	public abstract void addChannel(String name, ScriptChannelHandler handler);
}
