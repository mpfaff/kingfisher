package kingfisher.interop;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Thread-safe, structurally fixed object. Subject to the
 * <a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-17.html#jls-17.4">Java Memory Model</a>.
 */
public final class JObject implements ProxyObject {
	private final Map<String, Object> map;

	public JObject(Object obj) {
		switch (obj) {
			case Value valueIn -> {
				if (!valueIn.hasMembers()) throw new IllegalArgumentException("this value does not have members");
				this.map = new HashMap<>();
				valueIn.getMemberKeys().forEach(key -> map.put(key, valueIn.getMember(key)));
			}
			case Map<?, ?> mapIn -> {
				mapIn.forEach((k, v) -> {
					if (!(k instanceof String)) {
						throw new IllegalArgumentException("Expected a String key representing a field name, found " + k);
					}
				});
				//noinspection unchecked
				this.map = (Map<String, Object>) mapIn;
			}
			default ->
					throw new IllegalArgumentException("Cannot make a concurrent object from provided value: " + obj);
		}
	}

	private JObject(Map<String, Object> map, boolean _wrapped) {
		this.map = map;
	}

	public static JObject wrap(Map<String, Object> map) {
		return new JObject(map, true);
	}

	@Override
	public Object getMember(String key) {
		return map.get(key);
	}

	@Override
	public Object getMemberKeys() {
		return map.keySet();
	}

	@Override
	public boolean hasMember(String key) {
		return map.containsKey(key);
	}

	@Override
	public void putMember(String key, Value value) {
		if (!map.containsKey(key)) throw new IllegalArgumentException("No such field " + key + " on object");
		map.put(key, value);
	}

	@SuppressWarnings("MethodDoesntCallSuperMethod")
	@Override
	protected Object clone() {
		if (map instanceof HashMap<String, Object> hashMap) {
			return new JObject(hashMap.clone());
		}
		return new JObject(new HashMap<>(map));
	}

	@Override
	public int hashCode() {
		return map.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof JObject other && other.map.equals(map);
	}

	@Override
	public String toString() {
		return map.toString();
	}
}
