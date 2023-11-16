package kingfisher.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;

public final class BlockingOfferQueue<T> implements BlockingQueue<T> {
	private final BlockingQueue<T> delegate;

	public BlockingOfferQueue(BlockingQueue<T> delegate) {
		this.delegate = delegate;
	}

	@Override
	public boolean add(T t) {
		return delegate.add(t);
	}

	@Override
	public boolean offer(T t) {
		try {
			delegate.put(t);
			return true;
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void put(T t) throws InterruptedException {
		delegate.put(t);
	}

	@Override
	public boolean offer(T t, long timeout, TimeUnit unit) throws InterruptedException {
		return delegate.offer(t, timeout, unit);
	}

	@Override
	public T take() throws InterruptedException {
		return delegate.take();
	}

	@Override
	public T poll(long timeout, TimeUnit unit) throws InterruptedException {
		return delegate.poll(timeout, unit);
	}

	@Override
	public int remainingCapacity() {
		return delegate.remainingCapacity();
	}

	@Override
	public boolean remove(Object o) {
		return delegate.remove(o);
	}

	@Override
	public boolean contains(Object o) {
		return delegate.contains(o);
	}

	@Override
	public int drainTo(Collection<? super T> c) {
		return delegate.drainTo(c);
	}

	@Override
	public int drainTo(Collection<? super T> c, int maxElements) {
		return delegate.drainTo(c, maxElements);
	}

	@Override
	public T remove() {
		return delegate.remove();
	}

	@Override
	public T poll() {
		return delegate.poll();
	}

	@Override
	public T element() {
		return delegate.element();
	}

	@Override
	public T peek() {
		return delegate.peek();
	}

	@Override
	public int size() {
		return delegate.size();
	}

	@Override
	public boolean isEmpty() {
		return delegate.isEmpty();
	}

	@Override
	public Iterator<T> iterator() {
		return delegate.iterator();
	}

	@Override
	public Object[] toArray() {
		return delegate.toArray();
	}

	@Override
	public <T1> T1[] toArray(T1[] a) {
		return delegate.toArray(a);
	}

	@Override
	public <T1> T1[] toArray(IntFunction<T1[]> generator) {
		return delegate.toArray(generator);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return delegate.containsAll(c);
	}

	@Override
	public boolean addAll(Collection<? extends T> c) {
		return delegate.addAll(c);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return delegate.removeAll(c);
	}

	@Override
	public boolean removeIf(Predicate<? super T> filter) {
		return delegate.removeIf(filter);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return delegate.retainAll(c);
	}

	@Override
	public void clear() {
		delegate.clear();
	}

	@Override
	public boolean equals(Object o) {
		return delegate.equals(o);
	}

	@Override
	public int hashCode() {
		return delegate.hashCode();
	}

	@Override
	public Spliterator<T> spliterator() {
		return delegate.spliterator();
	}

	@Override
	public Stream<T> stream() {
		return delegate.stream();
	}

	@Override
	public Stream<T> parallelStream() {
		return delegate.parallelStream();
	}

	@Override
	public void forEach(Consumer<? super T> action) {
		delegate.forEach(action);
	}
}
