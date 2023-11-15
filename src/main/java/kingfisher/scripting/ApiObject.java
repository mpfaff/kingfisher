package kingfisher.scripting;

import java.util.function.BiConsumer;

/**
 * Implements the API available to a script.
 */
public class ApiObject  {
	protected final ScriptThread thread;

	public ApiObject(ScriptThread thread) {
		this.thread = thread;
	}

	/**
	 * Invokes the provided consumer with each exported value of this API.
	 */
	public void forEachExport(BiConsumer<String, Object> consumer) {
	}
}
