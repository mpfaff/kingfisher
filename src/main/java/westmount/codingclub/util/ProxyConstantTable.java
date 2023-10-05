package westmount.codingclub.util;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

public final class ProxyConstantTable implements ProxyObject {
	private static final Logger LOGGER = LoggerFactory.getLogger(ProxyConstantTable.class);

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
						LOGGER.warn("Skipping field " + field.getName() + " because it is not public");
						return false;
					}
					if (!Modifier.isFinal(modifiers)) {
						LOGGER.warn("Skipping field " + field.getName() + " because it is not final");
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
