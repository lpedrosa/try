package com.lpedrosa.util.function;

/**
 * Represent a supplier of results that might throw a Throwable.
 * <p>
 * Similar to Supplier, there is no requirement that a new or distinct result be
 * returned each time the supplier is invoked.
 * <p>
 * This is a functional interface whose functional method is {@link #get()}.
 *
 * @author lpedrosa
 * @param <T> the type of results supplied by this supplier
 */
@FunctionalInterface
public interface ThrowableSupplier<T> {
    /**
     * Gets a result.
     * @return a result
     * @throws Throwable if it failed to get a result
     */
    T get() throws Throwable;
}
