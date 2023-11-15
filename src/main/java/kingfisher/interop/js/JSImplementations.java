package kingfisher.interop.js;

import org.graalvm.polyglot.Source;

import java.util.List;
import java.util.Map;

public final class JSImplementations {
//	private static final Class<?> C_JSArrayBufferObject_DirectBase;
//	private static final Class<?> C_JSArrayBufferObject_Heap;
//	private static final MethodHandle MH_JSArrayBufferObject_Heap_getByteArray;
//	private static final MethodHandle MH_JSArrayBufferObject_DirectBase_getByteBuffer;
//
//	static {
//		try {
//			var l = MethodHandles.lookup();
//			C_JSArrayBufferObject_DirectBase = l.findClass("com.oracle.truffle.js.runtime.builtins.JSArrayBufferObject$DirectBase");
//			C_JSArrayBufferObject_Heap = l.findClass("com.oracle.truffle.js.runtime.builtins.JSArrayBufferObject$Heap");
//			MH_JSArrayBufferObject_DirectBase_getByteBuffer = l.findVirtual(C_JSArrayBufferObject_DirectBase,
//					"getByteBuffer",
//					methodType(ByteBuffer.class));
//			MH_JSArrayBufferObject_Heap_getByteArray = l.findVirtual(C_JSArrayBufferObject_Heap,
//					"getByteArray",
//					methodType(byte[].class));
//		} catch (ReflectiveOperationException e) {
//			throw new RuntimeException(e);
//		}
//	}

	public static final String FETCH_BODY_TO_NATIVE_VALUE = "fetchBodyToNativeValue";

	public static final List<Source> functions = Map.of(FETCH_BODY_TO_NATIVE_VALUE, """
								body => {
									const BodyPublishers = Java.type("java.net.http.HttpRequest.BodyPublishers");
									if (typeof body === "string") {
										return BodyPublishers.ofString(body);
									}
			//						else if (body instanceof Blob) {
			//							return BodyPublishers.ofByteArray(body.buffer);
			//						}
									else if (body instanceof ArrayBuffer) {
										// TODO: avoid this copy when possible
										return BodyPublishers.ofByteArray(Java.to(body, "byte[]"));
									}
			//						else if (body instanceof TypedArray) {
			//							return BodyPublishers.ofByteArray(body.buffer);
			//						}
			//						else if (body instanceof ReadableStream) {
			//							return BodyPublishers.ofInputStream(() => body);
			//						}
									else {
										throw new Error(`Unsupported body type: ${getMetaQualifiedName(body)}`);
									}
								}
								""").entrySet().stream().map(e -> Source.newBuilder("js",
			e.getValue(),
			e.getKey()).buildLiteral()).toList();

//	public static HttpRequest.BodyPublisher bodyPublisherOfArrayBuffer(Value arrayBufferValue) {
//		if (asOrNull(arrayBufferValue, C_JSArrayBufferObject_Heap) instanceof Object arrayBuffer) {
//			try {
//				return HttpRequest.BodyPublishers.ofByteArray((byte[]) MH_JSArrayBufferObject_Heap_getByteArray.invoke(
//						arrayBuffer));
//			} catch (Throwable e) {
//				throw new RuntimeException(e);
//			}
//		} else if (asOrNull(arrayBufferValue, C_JSArrayBufferObject_DirectBase) instanceof Object arrayBuffer) {
//			ByteBuffer byteBuffer;
//			try {
//				byteBuffer = (ByteBuffer) MH_JSArrayBufferObject_DirectBase_getByteBuffer.invoke(arrayBuffer);
//			} catch (Throwable e) {
//				throw new RuntimeException(e);
//			}
//			return HttpRequest.BodyPublishers.ofInputStream(() -> new ByteBufferInputStream(byteBuffer));
//		} else {
//			throw new IllegalArgumentException("Unsupported buffer type (subtype of ArrayBuffer) " + arrayBufferValue.getMetaObject().getMetaQualifiedName());
//		}
//	}
}
