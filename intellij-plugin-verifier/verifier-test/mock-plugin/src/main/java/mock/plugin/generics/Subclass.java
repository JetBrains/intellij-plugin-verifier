package mock.plugin.generics;

import generics.Base;

public class Subclass extends Base<Integer> {
  /*expected(PROBLEM)
  Abstract method generics.Base.foo(T arg0) : void is not implemented

  Concrete class mock.plugin.generics.Subclass inherits from generics.Base but doesn't implement the abstract method foo(T arg0) : void. This can lead to **AbstractMethodError** exception at runtime.
   */
  @Override
  public void foo(Integer number) {

  }
}
