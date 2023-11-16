package kingfisher.interop;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyIterator;

import java.util.Arrays;
import java.util.Spliterators;
import java.util.function.IntFunction;

public final class JArray implements ProxyArray {
	private final Object[] array;

	public JArray(int length) {
		this.array = new Object[length];
	}

	public JArray(Object[] array) {
		this.array = array;
	}

	private static <T> JArray boxed(int length, IntFunction<T> getter) {
		var a = new Object[length];
		for (int i = 0; i < a.length; i++) {
			a[i] = getter.apply(i);
		}
		return new JArray(a);
	}

	public static JArray from(byte[] array) {
		return boxed(array.length, i -> array[i]);
	}

	private int checkIndex(long index) {
		if (index < Integer.MIN_VALUE || index > Integer.MAX_VALUE) {
			throw new IndexOutOfBoundsException("Index is out of 32-bit integer range");
		}
		return (int) index;
	}

	@Override
	public Object get(long index) {
		return array[checkIndex(index)];
	}

	@Override
	public void set(long index, Value value) {
		array[checkIndex(index)] = value;
	}

	@Override
	public long getSize() {
		return array.length;
	}

	@Override
	public ProxyIterator getIterator() {
		return ProxyIterator.from(Spliterators.iterator(Arrays.spliterator(array)));
	}

	@Override
	public String toString() {
		return Arrays.toString(array);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(array);
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof JArray other && Arrays.equals(other.array, array);
	}
}
