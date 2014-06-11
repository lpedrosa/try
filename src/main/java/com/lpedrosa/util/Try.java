package com.lpedrosa.util;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A container object that represents a computation that might have failed
 * due to an exception. If the underlying computation resulted into an exception,
 * {@link #isFailure()} will return true and calling {@link #get()} will throw
 * the resulting exception.
 *
 * Additional methods of this class depend on the result of the wrapped computation.
 * Instances of this class are meant to be used by chaining them with methods,
 * using one of the provided high-order functions ({@link #map(Function)}, {@link #flatMap(Function)},
 * {@link #filter(Function)}).
 *
 * One can also recover from the underlying exceptions using either {@link #recover(Function)}
 * or {@link #recoverWith(Function)}.
 *
 * Instances of this class can be created by using one the following static methods:
 * {@link #of(ThrowableSupplier)}, {@link #success(Object)}, {@link #failure(Throwable)}
 *
 *
 * @author lpedrosa
 */
public final class Try<T> {

        private final T value;
        private final Optional<Throwable> error;

        public static <T> Try<T> of(ThrowableSupplier<T> supplier) {
            Objects.requireNonNull(supplier);

            T computationValue = null;
            Optional<Throwable> errorCase = Optional.empty();

            try {
                computationValue = supplier.get();
            } catch (Throwable t) {
                errorCase = Optional.of(t);
            }

            return new Try<>(computationValue, errorCase);
        }

        public static <T> Try<T> success(T value) {
            Objects.requireNonNull(value);

            return new Try<>(value, Optional.empty());
        }

        public static <T> Try<T> failure(Throwable t) {
            Objects.requireNonNull(t);

            return new Try<>(null, Optional.of(t));
        }

        public <U> Try<U> map(Function<? super T, ? extends U> mapper) {
            Objects.requireNonNull(mapper);

            return this.error.map(Try::<U>failure)
                             .orElse(Try.of(lazyApply(this.value, mapper)));
        }

        public <U> Try<U> flatMap(Function<? super T, Try<U>> mapper) {
            Objects.requireNonNull(mapper);

            return this.error.map(Try::<U>failure)
                             .orElse(mapper.apply(this.value));
        }

        public Try<T> filter(Predicate<? super T> predicate) {
            Objects.requireNonNull(predicate);

            if (error.isPresent() || predicate.test(this.value))
                return this;

            return Try.failure(new NoSuchElementException("Predicate does not hold for " + this.value));
        }

        public Try<T> recover(Function<Throwable, T> recoverFunc) {

            Objects.requireNonNull(recoverFunc);

            Optional<Try<T>> recoverFunctionIfError = this.error.map(throwable -> lazyApply(throwable, recoverFunc))
                                                                .map(Try::of);
            return recoverFunctionIfError.orElse(this);
        }

        public Try<T> recoverWith(Function<Throwable, Try<T>> recoverFunc) {
            Objects.requireNonNull(recoverFunc);

            Optional<Try<T>> recoverFunctionIfError = this.error.map(throwable -> recoverFunc.apply(throwable));
            return recoverFunctionIfError.orElse(this);
        }

        public T get() throws Throwable {

            if(this.error.isPresent()) {
                throw this.error.get();
            }
            return value;
        }

        public T orElse(T other) {
            return this.error.map(error -> other)
                             .orElse(this.value);
        }

        public Try<T> orElseGet(ThrowableSupplier<T> supplier) {
            return this.error.map(error -> supplier)
                             .map(Try::of)
                             .orElse(this);
        }

        public boolean isFailure() {
            return this.error.isPresent();
        }

        @Override
        public int hashCode() {
            this.error.map(throwable -> 0)
                      .orElse(this.value.hashCode());
            return 0;
        }

        @Override
        public boolean equals(Object obj) {
            return super.equals(obj);
        }

        @Override
        public String toString() {
            return this.error.map(throwable -> "Try.failure(" + throwable + ")")
                             .orElse("Try.success(" + this.value + ")");
        }

        private Try(T value, Optional<Throwable> error) {
            this.value = value;
            this.error = error;
        }

        private <X, U> ThrowableSupplier<U> lazyApply(X t, Function<X, ? extends U> recover) {
            return () -> recover.apply(t);
        }

        @FunctionalInterface
        public interface ThrowableSupplier<T> {
            T get() throws Throwable;
        }
}