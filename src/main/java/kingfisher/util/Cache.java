package kingfisher.util;

public interface Cache<K, V> {
	V get(K key);
}
