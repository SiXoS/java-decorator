package se.lindhen.decorators;

import se.lindhen.decorator.Decorator;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public abstract class DecoratedFuture<T> extends CompletableFuture<T> implements Decorator<CompletableFuture<T>> {

  public CompletableFuture<T> exceptionallyCompose(Function<Throwable, CompletableFuture<T>> handler) {
    getDelegate()
        .thenApply(var -> completedFuture(var))
        .exceptionally(handler)
        .thenCompose(future -> future);
  }
}