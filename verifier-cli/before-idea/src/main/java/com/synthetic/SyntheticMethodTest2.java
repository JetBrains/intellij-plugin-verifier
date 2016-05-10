package com.synthetic;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class SyntheticMethodTest2 {

  public static void main(String[] args) {
    A a = new A();
    a.x = 2;
    a.x();
    System.out.println(a.x);
    for (Method m : A.class.getDeclaredMethods()) {
      System.out.println(String.format("%08X", m.getModifiers()) + " " + m.getName());
    }
    System.out.println("--------------------------");
    for (Method m : A.class.getMethods()) {
      System.out.println(String.format("%08X", m.getModifiers()) + " " + m.getReturnType().getSimpleName() + " " + m.getName());
    }
    System.out.println("--------------------------");
    for (Constructor<?> c : A.class.getDeclaredConstructors()) {
      System.out.println(String.format("%08X", c.getModifiers()) + " " + c.getName());
    }
  }

  public static class A {
    private int x;

    private A() {
    }

    private void x() {
    }
  }
}