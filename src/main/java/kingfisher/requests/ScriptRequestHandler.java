package kingfisher.requests;

import kingfisher.responses.BuiltResponse;
import kingfisher.interop.JObject;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;

@FunctionalInterface
@HostAccess.Implementable
public interface ScriptRequestHandler {
	Value handle(ProxyRequest request, JObject arguments) throws Throwable;
}
