package mock.plugin.inheritance;

public class SubclassMultipleMethods extends MultipleMethods {
  /*expected(PROBLEM)
  Multiple default implementations of method mock.plugin.inheritance.MultipleMethods.foo() : void

  Method mock.plugin.inheritance.SubclassMultipleMethods.baz() : void contains an *invokespecial* instruction referencing a method reference mock.plugin.inheritance.MultipleMethods.foo() : void which has multiple default implementations: inheritance.MultipleDefaultMethod1.foo() : void and inheritance.MultipleDefaultMethod2.foo() : void. This can lead to **IncompatibleClassChangeError** exception at runtime.
  */
  public void baz() {
    super.foo();
  }
}
