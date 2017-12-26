package mock.plugin.inheritance;

import inheritance.AImpl;

public class AUser {
  public static void main(String[] args) {
    AImpl.createAImpl().foo();
  }
}
