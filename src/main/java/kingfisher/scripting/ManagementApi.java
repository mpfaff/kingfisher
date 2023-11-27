package kingfisher.scripting;

public final class ManagementApi {
	private static final String PERMISSION = "management";

	private final ScriptThread thread;

	public ManagementApi(ScriptThread thread) {
		this.thread = thread;
	}

	private static PermissionException makePermissionException() {
		return new PermissionException("This operation requires the '" + PERMISSION + "' permission");
	}

	public void reload() throws PermissionException {
		if (!thread.script.permissions.contains(PERMISSION)) {
			throw makePermissionException();
		}
		thread.engine.loader.hotReload();
	}
}
