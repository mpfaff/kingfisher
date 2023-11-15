/**
 * Kingfisher provides a robust (WIP) API to scripts.
 * <p>
 * <h2>Registration</h2>
 *
 * Scripts must register themselves to handle various events.
 *
 * <ul>
 *     <li>{@link kingfisher.scripting.ScriptThread.BaseApi#addRoute(String, String, ScriptRouteHandler)}</li>
 * </ul>
 *
 * <h2>Request handling</h2>
 *
 * See {@link kingfisher.requests.RequestScriptThread.Api}.
 */
package kingfisher;

import kingfisher.requests.ScriptRouteHandler;
