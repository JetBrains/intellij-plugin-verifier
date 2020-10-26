package mock.plugin.abstrackt;

import abstrakt.AbstractClass;
import abstrakt.Child;

/*expected(PROBLEM)
Abstract method abstrakt.AbstractClass.foo() : Parent is not implemented

Concrete class mock.plugin.abstrackt.AbstractClassImpl inherits from abstrakt.AbstractClass but doesn't implement the abstract method foo() : Parent. This can lead to **AbstractMethodError** exception at runtime.
*/
public class AbstractClassImpl extends AbstractClass {
  @Override
  public Child foo() {
    return null;
  }

  public static void main(String[] args) {
    AbstractClass main = new AbstractClassImpl();
    main.invoke();
//    main.foo();
  }
}
