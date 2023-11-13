package kingfisher;

import kingfisher.requests.MatchingRequestHandler;

import java.util.ArrayList;
import java.util.List;

public final class InitStagingArea {
	private boolean isComplete = false;
	public final List<MatchingRequestHandler> handlers = new ArrayList<>();

	public void checkState() {
		if (isComplete) throw new IllegalStateException("Attempt to use init api after init");
	}

	public void complete() {
		checkState();
		isComplete = true;
	}
}
