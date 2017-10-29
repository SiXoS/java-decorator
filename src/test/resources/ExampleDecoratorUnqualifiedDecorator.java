package se.lindhen.decorators;

import se.lindhen.decorator.Decorator;

import java.util.*;
import java.util.Optional;

public abstract class MapOptional<K,V> implements Map<K,V>, Decorator<Map<K,V>> {

  public Optional<V> find(K key) {
    return Optional.ofNullable(getOwner().get(key));
  }

  public static <K,V> MapOptional<K,V> decorate(Map<K,V> owner) {
    return null;
  }

}