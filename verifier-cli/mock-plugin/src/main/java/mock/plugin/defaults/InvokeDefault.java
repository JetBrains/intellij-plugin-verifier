package mock.plugin.defaults;

import defaults.B;

/**
 * @author Sergey Patrikeev
 */
public class InvokeDefault extends E implements B {
  public void bar() {
    //should not return a NotImplementedError
    B.super.foo();
  }
}
