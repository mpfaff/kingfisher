package westmount.codingclub.requests;

import westmount.codingclub.responses.BuiltResponse;

@FunctionalInterface
public interface RequestHandler {
	BuiltResponse handle(ProxyRequest request);
}
