package kingfisher.scripting;

import kingfisher.requests.RequestHandler;

import java.util.ArrayList;
import java.util.List;

public final class Registrar {
	private boolean isComplete = false;
	public final List<RequestHandler> requestHandlers = new ArrayList<>();

	public void checkState() {
		if (isComplete) throw new IllegalStateException("Attempt to use init api after init");
	}

	public void complete() {
		checkState();
		isComplete = true;
	}
}
