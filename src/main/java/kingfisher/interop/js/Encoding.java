package kingfisher.interop.js;

import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public enum Encoding {
	UTF_8(StandardCharsets.UTF_8);

	public static final Encoding DEFAULT = UTF_8;

	@Nullable
	public final Charset charset;

	private Encoding(@Nullable Charset charset) {
		this.charset = charset;
	}

	public String decode(byte[] bytes) {
		return charset.decode(ByteBuffer.wrap(bytes)).toString();
	}
}
