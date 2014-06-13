package com.lpedrosa.util;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import com.lpedrosa.util.function.ThrowableSupplier;

/**
 * A container object that represents a computation that might have failed
 * due to an exception. If the underlying computation resulted into an exception,
 * {@link #isFailure()} will return true and calling {@link #get()} will throw
 * the resulting exception.
 * <p>
 * Additional methods of this class depend on the result of the wrapped computation.
 * Instances of this class are meant to be used by chaining them with methods,
 * using one of the provided high-order functions ({@link #map(Function)},
 * {@link #flatMap(Function)}, {@link #filter(Predicate)}
 * <p>
 * For example, Try can be used to wrap a integer parsing operation when we are not sure about the source
 * of the input. If this operation fails, you can provide a default alternative.
 * Example:
 * <pre>
 * {@code
 * Try<String> sourceInput = obtainDubiousInput();
 * Integer parsedInt = sourceInput.map(Integer::parseInt)
 *                                .orElse(0);
 * }
 * </pre>
 * <p>
 * One can also recover from the underlying exceptions using either {@link #recover(Function)}
 * or {@link #recoverWith(Function)}.
 * <p>
 * Instances of this class can be created by using one the following static methods:
 * {@link #of(ThrowableSupplier)}, {@link #success(Object)}, {@link #failure(Throwable)}
 *
 * @author lpedrosa
 */
public final class Try<T> {

        private final T value;
        private final Optional<Throwable> error;

        /**
         * Returns a Try instance holding the value of the specified computation, if successful.
         * The Try might result in a failure if the supplier has thrown an exception.
         * @param <T> the class of the value
         * @param supplier a Supplier that might throw an exception, which must be non-null
         * @return a Try representing the success or failure of the provided computation
         * @throws NullPointerException if supplier is null
         */
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

        /**
         * Returns a Try instance, representing a success with the specified value
         * @param <T> the class of the value
         * @param value the successful value, which must be non-null
         * @return a success instance with the specified value
         * @throws NullPointerException if value is null
         */
        public static <T> Try<T> success(T value) {
            Objects.requireNonNull(value);

            return new Try<>(value, Optional.empty());
        }

        /**
         * Returns a Try instance, representing a failure with the specified throwable
         * @param <T> Type of the value, if a failure did not occur
         * @param t the throwable contained in this failure instance, which must be non-null
         * @return a failure instance with the specified throwable
         * @throws NullPointerException if t is null
         */
        public static <T> Try<T> failure(Throwable t) {
            Objects.requireNonNull(t);

            return new Try<>(null, Optional.of(t));
        }

        /**
         * Applies the provided mapping function to the value of this Try, if it represents a success.
         * Otherwise return this instance if it is a failure.
         * @param <U> The of the result of the mapping function
         * @param mapper a mapping function to apply to the value, if success
         * @return a Try describing the result of applying a mapping function to the value of
         * this try, if it represents a success, otherwise a failure Try instance
         * @throws NullPointerException if the mapping function is null
         */
        public <U> Try<U> map(Function<? super T, ? extends U> mapper) {
            Objects.requireNonNull(mapper);

            return this.error.map(Try::<U>failure)
                             .orElse(Try.of(lazyApply(this.value, mapper)));
        }

        /**
         * Applies the provided Try-bearing mapping function to the value of this Try, if it represents a sucsess.
         * Otherwise return this instance if it is a failure. This method is similar to {@link #map(Function)},
         * but the provided mapper is one whose result is already a Try, and if invoked, flatMap does not wrap
         * it with an additional Try.
         * @param <U> The type parameter to the Try returned by the mapping function
         * @param mapper a mapping function to apply to the value, if success
         * @return a Try describing the result of applying a Try-bearing mapping function to the value of this
         * try, if it is represents a succss, otherwise a failure Try instance
         * @throws NullPointerException if the mapping function is null
         */
        public <U> Try<U> flatMap(Function<? super T, Try<U>> mapper) {
            Objects.requireNonNull(mapper);

            return this.error.map(Try::<U>failure)
                             .orElse(mapper.apply(this.value));
        }

        /**
         * If this Try represents a success, and the value matches the given predicate, return a Try
         * describing the value, otherwise return a Try, representing a failure, wrapping a NoSuchElementException.
         * @param predicate a predicate to apply to the value, if success
         * @return a Try describing the value of this Try if success and the value matches the given predicate,
         * otherwise a Try describing a failure, wrapping a NoSuchElementException
         * @throws NullPointerException if the predicate is null
         */
        public Try<T> filter(Predicate<? super T> predicate) {
            Objects.requireNonNull(predicate);

            if (error.isPresent() || predicate.test(this.value))
                return this;

            return Try.failure(new NoSuchElementException("Predicate does not hold for " + this.value));
        }

        /**
         * Applies the provided recover function to the throwable of this try, if it is a failure.
         * Otherwise return this instance if this is a success.
         * @param recoverFunc a recover function to apply the throwable, if failure
         * @return a Try describing the result of applying a recover function to the throwable of
         * this try, if it represents a failure, otherwise a success Try instance
         * @throws NullPointerException if the recover function is null
         */
        public Try<T> recover(Function<Throwable, T> recoverFunc) {
            Objects.requireNonNull(recoverFunc);

            Optional<Try<T>> recoverFunctionIfError = this.error.map(throwable -> lazyApply(throwable, recoverFunc))
                                                                .map(Try::of);
            return recoverFunctionIfError.orElse(this);
        }

        /**
         * Applies the provided Try-bearing recover function to the throwable of this try, if it is a failure.
         * Otherwise return this instance if this is a success. This method is similar to {@link #recover(Function)},
         * but the provided recover function is one whose result is already a Try, and if invoked, recoverWith
         * does not wrap it with an additional Try.
         * @param recoverFunc a recover function to apply the throwable, if failure
         * @return a Try describing the result of applying a Try-bearing recover function to the throwable of
         * this try, if it represents a failure, otherwise a success Try instance
         * @throws NullPointerException if the recover function is null
         */
        public Try<T> recoverWith(Function<Throwable, Try<T>> recoverFunc) {
            Objects.requireNonNull(recoverFunc);

            Optional<Try<T>> recoverFunctionIfError = this.error.map(throwable -> recoverFunc.apply(throwable));
            return recoverFunctionIfError.orElse(this);
        }

        /**
         * If this Try represents a success, return the underlying value. Otherwise, throw the Throwable
         * associated with the failure.
         * @return the value held by this Try, if it represents a success
         * @throws Throwable if this represents a failure
         */
        public T get() throws Throwable {
            if(this.error.isPresent()) {
                throw this.error.get();
            }
            return value;
        }

        /**
         * Return the underlying value, if this represents a success. Otherwise return other
         * @param other the value to be returned if this represents a failure, may be null
         * @return the value, if success, otherwise other
         */
        public T orElse(T other) {
            return this.error.map(error -> other)
                             .orElse(this.value);
        }

        /**
         * Return the underlying value, if this represents a success. Otherwise invoke other
         * and return the result of that invocation wrapped in a Try.
         * @param other a Supplier whose result is return (wrapped in a Try) if this is a failure
         * @return the value, if success, otherwise the result of other.get() wrapped in a Try
         * @throws NullPointerException if other is null
         */
        public Try<T> orElseGet(ThrowableSupplier<T> other) {
            Objects.requireNonNull(other);
            return this.error.map(error -> other)
                             .map(Try::of)
                             .orElse(this);
        }

        /**
         * Return true if this represents a failure, otherwise false
         * @return true if this is a failure, otherwise false
         */
        public boolean isFailure() {
            return this.error.isPresent();
        }

        /**
         * Indicates whether some other object is "equal to" this Try. The other object is
         * considered equal if:
         * <ul>
         * <li>it is also a Try and;
         * <li>both instances are a failure and their Throwables are "equal to" each other via {@code equals()} or;
         * <li>both instances are a success and their values are "equal to" each other via {@code equals()}.
         * </ul>
         * @param obj an object to be tested for equality
         * @return if the other object is "equal to" this object otherwise false
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (!(obj instanceof Try)) {
                return false;
            }

            Try<?> other = (Try<?>) obj;
            if (this.isFailure() && other.isFailure()) {
                return Objects.equals(this.error, other.error);
            } else if (!this.isFailure() && !other.isFailure()) {
                return Objects.equals(this.value, other.value);
            }
            return false;
        }

        /**
         * Returns the hash code value of the underlying value, if this represents a success,
         * or 0 (zero) if no value is present.
         * @return hash code value of the underlying value or 0 if this represents a failure
         */
        @Override
        public int hashCode() {
            this.error.map(throwable -> 0)
                      .orElse(this.value.hashCode());
            return 0;
        }

        /**
         * Returns a non-empty string representation of this Try suitable for debugging.
         * @return a string representation of this instance
         */
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
}