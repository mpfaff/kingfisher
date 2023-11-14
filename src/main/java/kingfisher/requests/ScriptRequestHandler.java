package kingfisher.requests;

import kingfisher.responses.BuiltResponse;
import kingfisher.interop.JObject;
import org.graalvm.polyglot.HostAccess;

@FunctionalInterface
@HostAccess.Implementable
public interface ScriptRequestHandler {
	BuiltResponse handle(ProxyRequest request, JObject arguments) throws Exception;
}
