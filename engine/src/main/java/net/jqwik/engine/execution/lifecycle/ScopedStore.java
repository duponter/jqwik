package net.jqwik.engine.execution.lifecycle;

import java.util.function.*;
import java.util.logging.*;

import org.junit.platform.engine.*;

import net.jqwik.api.lifecycle.*;
import net.jqwik.engine.support.*;

import static net.jqwik.engine.support.JqwikStringSupport.*;

public class ScopedStore<T> implements Store<T> {

	private static final Logger LOG = Logger.getLogger(ScopedStore.class.getName());

	private final Object identifier;
	private final Lifespan lifespan;
	private final TestDescriptor scope;
	private final Supplier<T> initialValueSupplier;
	private final Consumer<T> onClose;

	private T value;
	private boolean initialized = false;

	public ScopedStore(
		Object identifier,
		Lifespan lifespan,
		TestDescriptor scope,
		Supplier<T> initialValueSupplier,
		Consumer<T> onClose
	) {
		this.identifier = identifier;
		this.lifespan = lifespan;
		this.scope = scope;
		this.initialValueSupplier = initialValueSupplier;
		this.onClose = onClose;
	}

	@Override
	public synchronized T get() {
		if (!initialized) {
			value = initialValueSupplier.get();
			initialized = true;
		}
		return value;
	}

	@Override
	public Lifespan lifespan() {
		return lifespan;
	}

	@Override
	public synchronized void update(Function<T, T> updater) {
		value = updater.apply(get());
	}

	@Override
	public synchronized void reset() {
		close();
		initialized = false;

		// Free memory as soon as possible, the store object might go live on for a while:
		value = null;
	}

	public Object getIdentifier() {
		return identifier;
	}

	public TestDescriptor getScope() {
		return scope;
	}

	public boolean isVisibleFor(TestDescriptor retriever) {
		return isInScope(retriever);
	}

	private boolean isInScope(TestDescriptor retriever) {
		if (retriever == scope) {
			return true;
		}
		return retriever.getParent().map(this::isInScope).orElse(false);
	}

	@Override
	public String toString() {
		return String.format(
			"Store(%s, %s, %s): [%s]",
			displayString(identifier),
			lifespan.name(),
			scope.getUniqueId(),
			displayString(value)
		);
	}

	public void close() {
		if (!initialized) {
			return;
		}
		closeOnReset();
		try {
			onClose.accept(value);
		} catch (Throwable throwable) {
			JqwikExceptionSupport.rethrowIfBlacklisted(throwable);
			String message = String.format("Exception while closing store [%s]", this);
			LOG.log(Level.SEVERE, message, throwable);
		}
	}

	private void closeOnReset() {
		if (value instanceof Store.CloseOnReset) {
			try {
				((Store.CloseOnReset) value).close();
			} catch (Throwable throwable) {
				JqwikExceptionSupport.rethrowIfBlacklisted(throwable);
				String message = String.format("Exception while closing store [%s]", this);
				LOG.log(Level.SEVERE, message, throwable);
			}
		}
	}

}

