package kingfisher.scripting;

public abstract non-sealed class LiveRegistrationApi extends RegistrationApi {
	protected abstract ScriptThread thread();

	@Override
	public void requestPermission(String permission) throws PermissionException {
		if (!thread().script.permissions.contains(permission)) {
			throw new PermissionException("Permission '" + permission + "' not granted");
		}
	}
}
