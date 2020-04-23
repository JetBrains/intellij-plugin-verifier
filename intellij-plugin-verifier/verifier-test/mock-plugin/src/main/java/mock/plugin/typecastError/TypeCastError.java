package mock.plugin.typecastError;

import typecastError.Base;
import typecastError.Derived;
import typecastError.MethodWithBaseParameter;

public class TypeCastError {
  public static void error() {
    Base derived = new Derived();
    MethodWithBaseParameter.method(derived);

    String s = (String) new Object();
    System.out.println(s);
  }

  public static void main(String[] args) {
    error();
  }
}
