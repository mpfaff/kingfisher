package westmount.codingclub.util;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Structurally fixed object.
 */
public final class ConcurrentObject implements ProxyObject {
	private final Map<String, Object> map = new HashMap<>();

	public ConcurrentObject(Object obj) {
		switch (obj) {
			case Value valueIn -> {
				if (!valueIn.hasMembers()) throw new IllegalArgumentException("this value does not have members");
				valueIn.getMemberKeys().forEach(key -> map.put(key, valueIn.getMember(key)));
			}
			case Map<?, ?> mapIn -> mapIn.forEach((k, v) -> this.map.put((String) k, v));
			default -> throw new IllegalArgumentException("Cannot make a concurrent object from provided value: " + obj);
		}
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
