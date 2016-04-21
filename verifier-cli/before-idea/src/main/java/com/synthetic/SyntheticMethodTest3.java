package com.synthetic;

import java.util.LinkedList;
import java.util.List;

public class SyntheticMethodTest3 {

  @SuppressWarnings("unchecked")
  public static void main(String[] args) {
    List<?> a = new MyLink();
    Object z = a.get(0);

    List b = new MyLink();
    b.add("");
    b.add(13);
  }

  public static class MyLink extends LinkedList<String> {

    @Override
    public boolean add(String s) {
      return true;
    }

    @Override
    public String get(int i) {
      return "";
    }
  }
}