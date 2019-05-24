package mock.plugin.defaults;

import defaults.B;

public class InvokeDefault extends E implements B {
  public void bar() {
    //should not return a NotImplementedError
    B.super.foo();
  }
}
