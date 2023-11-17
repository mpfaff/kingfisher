package kingfisher.interop;

import dev.pfaff.log4truth.Logger;
import kingfisher.interop.js.PromiseRejectionException;
import kingfisher.responses.BuiltResponse;
import kingfisher.scripting.EventLoop;
import kingfisher.util.Errors;
import org.graalvm.polyglot.Value;

import java.lang.invoke.*;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;

import static dev.pfaff.log4truth.StandardTags.DEBUG;
import static java.lang.invoke.MethodHandles.*;
import static java.lang.invoke.MethodType.methodType;

public final class ValueUtil {
	public static <T> T asOrNull(Value value, Class<?> targetType) {
		try {
			return (T) value.as(targetType);
		} catch (Throwable e) {
			return null;
		}
	}

	public static <R extends Record> Function<Value, R> recordConverter(Class<R> targetType) {
		var components = targetType.getRecordComponents();
		var l = MethodHandles.lookup();
		var MT_apply = methodType(Object.class, Object.class);
		MethodHandle MH_apply;
		try {
			MH_apply = l.findVirtual(Function.class, "apply", MT_apply);
		} catch (NoSuchMethodException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
		var getters = Arrays.stream(components).map(component -> {
			Function<Value, Object> getter;
			if (component.isAnnotationPresent(OptionalField.class)) {
				getter = v -> {
					var name = component.getName();
					if (v.hasMember(name)) {
						return v.getMember(name);
					} else if (v.hasHashEntry(name)) {
						return v.getHashValue(name);
					} else {
						return null;
					}
				};
			} else {
				getter = v -> {
					var name = component.getName();
					Object value;
					if (v.hasMember(name)) {
						value = v.getMember(name);
					} else if (v.hasHashEntry(name)) {
						value = v.getHashValue(name);
					} else {
						value = null;
					}
					if (value == null) {
						throw new IllegalArgumentException("Field '" + name + "' must have a non-null value");
					}
					return value;
				};
			}
			return MH_apply.bindTo(getter).asType(methodType(component.getType(), Value.class));
		}).toArray(MethodHandle[]::new);
		MethodHandle constructor;
		try {
			constructor = permuteArguments(filterArguments(l.findConstructor(targetType,
							methodType(void.class,
									Arrays.stream(components).<Class<?>>map(RecordComponent::getType).toList())),
					0,
					getters), methodType(targetType, Value.class), IntStream.range(0, getters.length).toArray());
		} catch (NoSuchMethodException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
		try {
			return (Function<Value, R>) LambdaMetafactory.metafactory(l,
							"apply",
							methodType(Function.class, MethodHandle.class),
							MT_apply,
							MethodHandles.exactInvoker(methodType(targetType, Value.class)),
							MT_apply.changeParameterType(0, Value.class).changeReturnType(targetType))
					.getTarget()
					.invokeExact(constructor);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	public static Object resolveMaybePromise(Value value, EventLoop eventLoop) throws Exception {
		if (value.canInvokeMember("then")) {
			var fut = new CompletableFuture<>();
			Logger.log(() -> "registered then callback " + System.identityHashCode(fut),
					List.of(DEBUG));
			Consumer<Object> resolveFn = v -> {
				Logger.log(() -> "resolved " + System.identityHashCode(fut), List.of(DEBUG));
				fut.complete(v);
			};
			Consumer<Object> rejectFn = e -> {
				Logger.log(() -> "rejected " + System.identityHashCode(fut), List.of(DEBUG));
				fut.completeExceptionally(new PromiseRejectionException(e));
			};
			value.invokeMember("then", resolveFn, rejectFn);
			//					result.invokeMember("then", resolveFn);
			//					result.invokeMember("catch", rejectFn);
			while (!fut.isDone()) {
				if (eventLoop.runMicrotask()) {
					Logger.log(() -> "ran microtask", List.of(DEBUG));
				} else {
					Thread.onSpinWait();
				}
			}
			Logger.log(() -> "completed", List.of(DEBUG));
			try {
				return fut.get();
			} catch (Throwable e) {
				throw Errors.wrapError(Errors.unwrapError(e));
			}
		} else {
			return value;
		}
	}
}
