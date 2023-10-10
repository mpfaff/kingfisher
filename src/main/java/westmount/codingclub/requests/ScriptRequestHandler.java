package westmount.codingclub.requests;

import westmount.codingclub.responses.BuiltResponse;
import westmount.codingclub.util.JObject;

@FunctionalInterface
public interface ScriptRequestHandler {
	BuiltResponse handle(ProxyRequest request, JObject arguments) throws Exception;
}
