package kingfisher.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class MapCache<K, V> implements Cache<K, V> {
	private final Map<K, V> map = new ConcurrentHashMap<>();
	private final Function<K, V> computer;

	public MapCache(Function<K, V> computer) {
		this.computer = computer;
	}

	@Override
	public V get(K key) {
		return map.computeIfAbsent(key, computer);
	}
}
