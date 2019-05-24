package mock.plugin.generics;

import generics.Base;

public class NoSuchMethodError {
  public void error(Base<Integer> base) {
    //after change of the Base<T extends Number> to Base<T>
    // the signature of the method foo has changed from foo(Ljava/lang/Number;)V to foo(Ljava/lang/Object;)V
    base.foo(123);
  }
}
