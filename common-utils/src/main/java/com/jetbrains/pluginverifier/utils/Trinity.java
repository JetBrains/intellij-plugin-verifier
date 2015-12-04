package com.jetbrains.pluginverifier.utils;

public class Trinity<A, B, C> {
  public final A first;
  public final B second;
  public final C third;

  public Trinity(A first, B second, C third) {
    this.third = third;
    this.first = first;
    this.second = second;
  }

  public static <A, B, C> Trinity<A, B, C> create(A first, B second, C third) {
    return new Trinity<A, B, C>(first, second, third);
  }

  public final A getFirst() {
    return first;
  }

  public final B getSecond() {
    return second;
  }

  public C getThird() {
    return third;
  }

  @SuppressWarnings("SimplifiableIfStatement")
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Trinity<?, ?, ?> trinity = (Trinity<?, ?, ?>) o;

    if (first != null ? !first.equals(trinity.first) : trinity.first != null) return false;
    if (second != null ? !second.equals(trinity.second) : trinity.second != null) return false;
    return !(third != null ? !third.equals(trinity.third) : trinity.third != null);

  }

  @Override
  public int hashCode() {
    int result = first != null ? first.hashCode() : 0;
    result = 31 * result + (second != null ? second.hashCode() : 0);
    result = 31 * result + (third != null ? third.hashCode() : 0);
    return result;
  }

  public String toString() {
    return "<" + first + "," + second + "," + third + ">";
  }
}