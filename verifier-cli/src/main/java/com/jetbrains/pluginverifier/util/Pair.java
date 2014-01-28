package com.jetbrains.pluginverifier.util;

import org.jetbrains.annotations.Nullable;

/**
 * @author Sergey Evdokimov
 */
public class Pair<A, B> {

  public final A first;
  public final B second;

  public Pair(@Nullable A first, @Nullable B second) {
    this.first = first;
    this.second = second;
  }

  public A getFirst() {
    return first;
  }

  public B getSecond() {
    return second;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Pair)) return false;

    Pair pair = (Pair)o;

    if (first != null ? !first.equals(pair.first) : pair.first != null) return false;
    if (second != null ? !second.equals(pair.second) : pair.second != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = first != null ? first.hashCode() : 0;
    result = 31 * result + (second != null ? second.hashCode() : 0);
    return result;
  }

  public static <A, B> Pair<A,B> create(A first, B second) {
    return new Pair<A, B>(first, second);
  }
}
