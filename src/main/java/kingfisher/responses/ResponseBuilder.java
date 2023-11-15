package kingfisher.responses;

import kingfisher.constants.ContentType;
import kingfisher.constants.Header;
import kingfisher.constants.Status;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class ResponseBuilder {
	private int status;
	private Map<String, String> headers = null;
	private Writable writable = null;

	public ResponseBuilder() {
		this(Status.OK);
	}

	public ResponseBuilder(int status) {
		this.status = status;
	}

	/**
	 * Sets the status code of the response.
	 */
	public ResponseBuilder status(int statusCode) {
		status = statusCode;
		return this;
	}

	/**
	 * Sets the body of the response.
	 */
	public ResponseBuilder content(Writable writable) {
		this.writable = writable;
		return this;
	}

	/**
	 * Sets the body of the response to a {@link Writable} wrapping the provided byte array.
	 */
	public ResponseBuilder content(byte[] content) {
		return content(Writable.bytesWriter(content));
	}

	/**
	 * Sets the body of the response to a {@link Writable} wrapping the provided string.
	 */
	public ResponseBuilder content(String content) {
		return content(Writable.stringWriter(content));
	}

	private Map<String, String> getHeaders() {
		if (this.headers == null) return this.headers = new HashMap<>();
		return this.headers;
	}

	/**
	 * Appends the headers to the response.
	 */
	public ResponseBuilder headers(Map<String, String> headers) {
		getHeaders().putAll(headers);
		return this;
	}

	/**
	 * Appends the header to the response.
	 */
	public ResponseBuilder header(String key, String value) {
		getHeaders().put(key, value);
		return this;
	}

	/**
	 * Appends the {@code Content-Type} header to the response.
	 */
	public ResponseBuilder contentType(String value) {
		return header(Header.CONTENT_TYPE, value);
	}

	public ResponseBuilder html() {
		return contentType(ContentType.TEXT_HTML);
	}

	/**
	 * Appends the {@code Location} header to the response.
	 */
	public ResponseBuilder location(String value) {
		return header(Header.LOCATION, value);
	}

	public BuiltResponse finish() {
		return new BuiltResponse(status, Objects.requireNonNullElseGet(headers, Map::of), writable);
	}
}
