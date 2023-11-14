package kingfisher.interop;

import dev.pfaff.log4truth.Logger;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import static dev.pfaff.log4truth.StandardTags.WARN;

public final class ProxyConstantTable implements ProxyObject {
	private final Map<String, Object> entries;

	public ProxyConstantTable(Map<String, ?> entries) {
		this.entries = Map.copyOf(entries);
	}

	public ProxyConstantTable(Class<?> tableClazz) {
		this(Arrays.stream(tableClazz.getDeclaredFields())
				.filter(field -> Modifier.isStatic(field.getModifiers()))
				.filter(field -> {
					var modifiers = field.getModifiers();
					if (!Modifier.isPublic(modifiers)) {
						Logger.log(() -> "Skipping field " + field.getName() + " because it is not public", List.of(WARN));
						return false;
					}
					if (!Modifier.isFinal(modifiers)) {
						Logger.log(() -> "Skipping field " + field.getName() + " because it is not final", List.of(WARN));
						return false;
					}
					return true;
				})
				.collect(Collectors.toUnmodifiableMap(Field::getName, field -> {
					try {
						return field.get(null);
					} catch (IllegalAccessException e) {
						throw new RuntimeException(e);
					}
				})));
	}

	@Override
	public Object getMember(String key) {
		if (!entries.containsKey(key)) throw new NoSuchElementException("No such field in constant table: " + key);
		return entries.get(key);
	}

	@Override
	public Object getMemberKeys() {
		return entries.keySet();
	}

	@Override
	public boolean hasMember(String key) {
		return entries.containsKey(key);
	}

	@Override
	public void putMember(String key, Value value) {
		throw new UnsupportedOperationException("Cannot put key-value pair in constant table");
	}
}
