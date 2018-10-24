package pkg;

import java.util.List;
import java.util.Map;

class A<F> {

  public int f1;
  public String f2;
  public List<String> f3;
  public F f4;

  public A() {
  }

  public void m1() {
  }

  public int m2() {
    return 0;
  }

  public String m3() {
    return "";
  }

  public void m4(String s) {
  }

  public void m5(List<String> l) {
  }

  public <T> T m6() {
    return null;
  }

  public <T> void m7(T t) {
  }

  public <T extends Number, S extends T> T m8(S s) {
    return s;
  }

  public void m9(Map<String, Integer> m) {
  }

  public <K, V> void m10(Map<K, V> m) {
  }

  public int[] m11() {
    return null;
  }

  public String[][] m12() {
    return null;
  }

  public List<Comparable<? extends Number>> m13() {
    return null;
  }

  public <E extends Class> Class<? extends E> m14() {
    return null;
  }

  public <E extends Class> Class<? super E> m15() {
    return null;
  }

  public <E, T extends Comparable<E>> Map<Object, String> m16(Object o, Map<Object, String> m, T t) {
    return null;
  }

  public void m17(Class<?> c, Class<?>[][] cs) {
  }


}