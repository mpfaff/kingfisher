package kingfisher.interop;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Thread-safe but non-synchronized, structurally fixed object. Subject to the Java Memory Model.s
 */
public final class JObject implements ProxyObject {
	private final Map<String, Object> map;

	public JObject(Object obj) {
		this.map = new HashMap<>();
		switch (obj) {
			case Value valueIn -> {
				if (!valueIn.hasMembers()) throw new IllegalArgumentException("this value does not have members");
				valueIn.getMemberKeys().forEach(key -> map.put(key, valueIn.getMember(key)));
			}
			case Map<?, ?> mapIn -> mapIn.forEach((k, v) -> this.map.put((String) k, v));
			default -> throw new IllegalArgumentException("Cannot make a concurrent object from provided value: " + obj);
		}
	}

	private JObject(Map<String, Object> map) {
		this.map = map;
	}

	public static JObject wrap(Map<String, Object> map) {
		return new JObject(map);
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
}
