package kingfisher.scripting;

public interface ScriptThreadGuard extends AutoCloseable {
	@Override
	void close();
}
