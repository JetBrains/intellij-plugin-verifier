package mock.plugin.generics;

import generics.Base;

public class NoSuchMethodError {
  /*expected(PROBLEM)
  Invocation of unresolved method generics.Base.foo(Number) : void

  Method mock.plugin.generics.NoSuchMethodError.error(generics.Base base) : void contains an *invokevirtual* instruction referencing an unresolved method generics.Base.foo(java.lang.Number) : void. This can lead to **NoSuchMethodError** exception at runtime.
   */
  public void error(Base<Integer> base) {
    //after change of the Base<T extends Number> to Base<T>
    // the signature of the method foo has changed from foo(Ljava/lang/Number;)V to foo(Ljava/lang/Object;)V
    base.foo(123);
  }
}
