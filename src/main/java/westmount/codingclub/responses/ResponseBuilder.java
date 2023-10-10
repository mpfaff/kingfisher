package westmount.codingclub.responses;

import westmount.codingclub.constants.ContentType;
import westmount.codingclub.constants.Header;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class ResponseBuilder {
	private int status = 200;
	private Map<String, String> headers = null;
	private Writer writer = null;

	/**
	 * Sets the status code of the response.
	 */
	public ResponseBuilder status(int statusCode) {
		status = statusCode;
		return this;
	}

	/**
	 * Sets the bytes writer of the response.
	 */
	public ResponseBuilder content(Writer writer) {
		this.writer = writer;
		return this;
	}

	/**
	 * Sets the bytes of the response to a writer wrapping the provided byte array.
	 */
	public ResponseBuilder content(byte[] content) {
		return content(Writer.bytesWriter(content));
	}

	/**
	 * Sets the bytes of the response to a writer wrapping the provided string.
	 */
	public ResponseBuilder content(String content) {
		return content(Writer.stringWriter(content));
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
	 * Appends the Content-Type header to the response.
	 */
	public ResponseBuilder contentType(String value) {
		return header(Header.CONTENT_TYPE, value);
	}

	public ResponseBuilder html() {
		return contentType(ContentType.TEXT_HTML);
	}

	/**
	 * Appends the Location header to the response.
	 */
	public ResponseBuilder location(String value) {
		return header(Header.LOCATION, value);
	}

	public BuiltResponse finish() {
		return new BuiltResponse(status, Objects.requireNonNullElseGet(headers, Map::of), writer);
	}
}
