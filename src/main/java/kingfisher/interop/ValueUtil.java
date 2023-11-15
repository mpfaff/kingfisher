package kingfisher.interop;

import org.graalvm.polyglot.Value;

import java.lang.invoke.*;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.IntStream;

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
}
