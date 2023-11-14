package kingfisher.interop;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyIterator;

import java.util.Arrays;
import java.util.Spliterators;

public final class JArray implements ProxyArray {
	private final Object[] array;

	public JArray(Object[] array) {
		this.array = array;
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
}
