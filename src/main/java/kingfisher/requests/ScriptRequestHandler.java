package kingfisher.requests;

import kingfisher.interop.JObject;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;

@FunctionalInterface
@HostAccess.Implementable
public interface ScriptRequestHandler {
	Value handle(HttpServerRequest request, JObject arguments) throws Throwable;
}
