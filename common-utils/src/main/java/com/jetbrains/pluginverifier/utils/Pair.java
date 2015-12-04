package com.jetbrains.pluginverifier.utils;

/**
 * @author Sergey Evdokimov
 */
public class Pair<A, B> {

  public final A first;
  public final B second;

  public Pair(A first, B second) {
    this.first = first;
    this.second = second;
  }

  public static <A, B> Pair<A, B> create(A first, B second) {
    return new Pair<A, B>(first, second);
  }

  public A getFirst() {
    return first;
  }

  public B getSecond() {
    return second;
  }

  @SuppressWarnings("SimplifiableIfStatement")
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Pair)) return false;

    Pair pair = (Pair) o;

    if (first != null ? !first.equals(pair.first) : pair.first != null) return false;
    return !(second != null ? !second.equals(pair.second) : pair.second != null);

  }

  @Override
  public int hashCode() {
    int result = first != null ? first.hashCode() : 0;
    result = 31 * result + (second != null ? second.hashCode() : 0);
    return result;
  }
}
