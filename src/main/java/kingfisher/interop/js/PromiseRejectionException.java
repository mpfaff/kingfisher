package kingfisher.interop.js;

public final class PromiseRejectionException extends Exception {
	public final Object value;

	public PromiseRejectionException(Object value) {
		super("Promise rejected", value instanceof Throwable t ? t : null);
		this.value = value;
	}

	@Override
	public String toString() {
		if (value instanceof Throwable) return super.toString();
		return getMessage() + ": " + value;
	}
}
