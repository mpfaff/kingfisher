package kingfisher.templating;

import io.pebbletemplates.pebble.error.LoaderException;
import io.pebbletemplates.pebble.loader.Loader;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class FileLoader implements Loader<String> {
	private final Path root;

	public FileLoader(Path root) {
		this.root = Objects.requireNonNull(root).toAbsolutePath();
	}

	@Override
	public Reader getReader(String cacheKey) {
		var path = root.resolve(cacheKey).toAbsolutePath();
		if (!path.startsWith(root)) {
			throw new IllegalArgumentException("no escaping the root");
		}
		try {
			return Files.newBufferedReader(path);
		} catch (IOException e) {
			throw new LoaderException(e, "Unable to load template file \"" + path + "\"");
		}
	}

	@Override
	public void setCharset(String charset) {
	}

	@Override
	public void setPrefix(String prefix) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setSuffix(String suffix) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String resolveRelativePath(String relativePath, String anchorPath) {
		return Path.of(anchorPath).resolve(relativePath).toString();
	}

	@Override
	public String createCacheKey(String templateName) {
		return templateName;
	}

	@Override
	public boolean resourceExists(String templateName) {
		try {
			getReader(createCacheKey(templateName)).close();
			return true;
		} catch (Throwable e) {
			return false;
		}
	}
}
