package kingfisher.scripting;

import kingfisher.requests.RequestHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Registrar {
	private boolean isComplete = false;
	public final List<RequestHandler> requestHandlers = new ArrayList<>();
	public final Map<String, HandlerRef> channelHandlers = new HashMap<>();

	public void checkState() {
		if (isComplete) throw new IllegalStateException("Attempt to use init api after init");
	}

	public void complete() {
		checkState();
		isComplete = true;
	}
}
