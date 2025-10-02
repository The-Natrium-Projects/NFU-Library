package net.sodiumzh.nfu.object;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Representing an object accessible only after validation. For preventing accident access before initialization etc.
 */
public final class Validatable<T> {

    private boolean validated = false;
    private T value;
    private Supplier<? extends RuntimeException> exceptionSupplier;

    public Validatable(T value, Supplier<? extends RuntimeException> exceptionSupplier) {
        this.value = value;
        this.exceptionSupplier = exceptionSupplier;
    }

    public Validatable(T value) {
        this(value, () -> new IllegalStateException("Access before validation"));
    }

    public Validatable() {this(null);}

    /**
     * Get the value if validated, or throw if not validated.
     */
    public T get() {
        if (!validated) throw exceptionSupplier.get();
        return value;
    }

    /**
     * Set the value, no matter if it's validated.
     */
    public void set(T value) {
        this.value = value;
    }

    /**
     * Apply a modification to the value, no matter if it's validated.
     */
    public void modify(Consumer<? super T> modification) {
        modification.accept(value);
    }

    /**
     * Label this object as validated and accessible.
     */
    public void validate() {
        this.validated = true;
    }

    /**
     * Label this object as not validated, and thus inaccessible.
     */
    public void invalidate() { this.validated = false; }

    /**
     * Set the value and validated this object. Note that the value will still be set
     * even if this Validatable is already validated.
     */
    public Validatable<T> setAndValidate(T value) {
        this.value = value;
        this.validated = true;
        return this;
    }

    /**
     * Apply a modification to the value and validated this object. Note that the value will still be modified
     * even if this Validatable is already validated.
     */
    public Validatable<T> modifyAndValidate(Consumer<? super T> modification) {
        modification.accept(value);
        this.validated = true;
        return this;
    }

    /**
     * Set the value and validate this object if it's not validated. If it's already validated, do nothing.
     * @return {@code this}, allowing chain initialization and access,
     * like {@code validatable.setIfInvalid(value).get()}.
     */
    public Validatable<T> setIfInvalid(T value) {
        if (!this.validated) {
            this.value = value;
            this.validated = true;
        }
        return this;
    }

    /**
     * Set the value from supplier and validate this object if it's not validated. If it's already validated, do nothing.
     * @return {@code this}, allowing chain initialization and access,
     * like {@code validatable.setIfInvalid(supplier).get()}.
     */
    public Validatable<T> setIfInvalid(Supplier<T> supplier) {
        if (!this.validated) {
            this.value = supplier.get();
            this.validated = true;
        }
        return this;
    }

    /**
     * Whether this object is validated.
     */
    public boolean isValidated() {
        return this.validated;
    }

    /**
     * Get an {@link Optional} of the value if validated, or empty if not.
     * <p>Note: this action cannot distinguish validated null value with the invalidated state.
     */
    public Optional<T> getIfValidated() {
        if (!validated) return Optional.empty();
        else return Optional.ofNullable(value);
    }

}
