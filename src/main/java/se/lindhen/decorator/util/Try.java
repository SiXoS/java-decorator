package se.lindhen.decorator.util;

import se.lindhen.decorator.classmanipulator.ClassInfoReader;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class Try<T> {

  private static final Try<?> EMPTY = new Try<>();

  private final T value;
  private final String error;

  private Try() {
    this.value = null;
    this.error = null;
  }

  public static <T> Try<T> empty() {
    @SuppressWarnings("unchecked")
    Try<T> t = (Try<T>) EMPTY;
    return t;
  }

  private Try(T value) {
    this.value = Objects.requireNonNull(value);
    this.error = null;
  }

  private Try(String failureReason) {
    this.value = null;
    this.error = failureReason;
  }

  public static <T> Try<T> of(T value) {
    return new Try<>(value);
  }

  public static <T> Try<T> ofNullable(T value) {
    return value == null ? empty() : of(value);
  }

  public static <T> Try<T> failed(String failureReason) {
    return new Try<>(failureReason);
  }

  public static <T> Try<T> ofOptional(Optional<T> optional) {
    return optional.map(Try::of).orElse(empty());
  }

  public static <T> Try<T> ofOptional(Optional<T> optional, String failureIfOptionalIsEmpty) {
    return ofOptional(optional).failOnEmpty(failureIfOptionalIsEmpty);
  }

  public T get() {
    if (value == null) {
      throw new NoSuchElementException("No value present");
    }
    return value;
  }

  public boolean isPresent() {
    return value != null;
  }

  public Try<T> runIfPresent(Consumer<? super T> consumer) {
    if(isPresent()){
      consumer.accept(value);
    }
    return this;
  }

  public void ifPresent(Consumer<? super T> consumer) {
    if (value != null)
      consumer.accept(value);
  }

  public void ifFailed(Consumer<String> failureHandler) {
    if(hasFailed()){
      failureHandler.accept(error);
    }
  }

  public boolean hasFailed() {
    return error != null;
  }

  public Try<T> filter(Predicate<? super T> predicate) {
    if(hasFailed()) {
      return this;
    }
    Objects.requireNonNull(predicate);
    if (!isPresent())
      return this;
    else
      return predicate.test(value) ? this : empty();
  }

  public <U> Try<U> map(Function<? super T, ? extends U> mapper) {
    if(hasFailed()) {
      return failed(error);
    }
    Objects.requireNonNull(mapper);
    if (!isPresent())
      return empty();
    else {
      return Try.ofNullable(mapper.apply(value));
    }
  }

  public <U> Try<U> flatMap(Function<? super T, Try<U>> mapper) {
    if(hasFailed()) {
      return failed(error);
    }
    Objects.requireNonNull(mapper);
    if (!isPresent())
      return empty();
    else {
      return Objects.requireNonNull(mapper.apply(value));
    }
  }

  public <U> Try<U> flatMapOptional(Function<? super T, Optional<U>> mapper) {
    if(hasFailed()) {
      return failed(error);
    }
    Objects.requireNonNull(mapper);
    if(!isPresent()) {
      return empty();
    }else {
      return ofOptional(Objects.requireNonNull(mapper.apply(value)));
    }
  }

  public T orElse(T other) {
    return value != null ? value : other;
  }

  public T orElseGet(Supplier<? extends T> other) {
    return value != null ? value : other.get();
  }

  public Try<T> failOnEmpty(String failureReason) {
    if(hasFailed()){
      return this;
    }
    if (value == null) {
      return failed(failureReason);
    } else {
      return this;
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof Try)) {
      return false;
    }

    Try<?> other = (Try<?>) obj;
    return Objects.equals(value, other.value);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(value);
  }

  @Override
  public String toString() {
    if(error != null) {
      return String.format("Try[failed: '%s']", error);
    }else if (value != null) {
      return String.format("Try[%s]", value);
    }else {
      return "Try.empty";
    }
  }

  /**
   * Turns a list of attempts into an attempt with the list of al elements.
   * If any attempt failed, the error for that attempt is returned as a failed attempt.
   * Any empty attempts is ignored.
   * If all attempts were empty, the resulting attempt will contain an empty list.
   *
   * examples:
   * {try(1), try(2), try(3)} -> try({1,2,3})
   * {try(1), failed(error1), failed(error2), try(4)} -> failed(error1)
   * {try(1), empty, try(3)} -> try({1,3})
   * {empty, empty} -> try({})
   * @param tries
   * @param <T>
   * @return
   */
  public static <T> Try<List<T>> join(List<Try<T>> tries) {
    List<T> elems = new ArrayList<>(tries.size());
    for (Try<T> attempt : tries) {
      if(attempt.hasFailed()) {
        return failed(attempt.error);
      } else if(attempt.isPresent()) {
        elems.add(attempt.value);
      }
    }
    return of(elems);
  }
}
