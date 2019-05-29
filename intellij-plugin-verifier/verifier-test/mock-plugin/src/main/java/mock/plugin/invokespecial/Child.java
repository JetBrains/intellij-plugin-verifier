package mock.plugin.invokespecial;

import invokespecial.AbstractParent;

/*expected(PROBLEM)
Attempt to invoke an abstract method invokespecial.AbstractParent.foo() : void

Method mock.plugin.invokespecial.Child.bar() : void contains an *invokespecial* instruction referencing a method invokespecial.AbstractParent.foo() : void which doesn't have an implementation. This can lead to **AbstractMethodError** exception at runtime.
*/

/*expected(PROBLEM)
Attempt to invoke an abstract method invokespecial.SuperInterface.deletedBody() : void

Method mock.plugin.invokespecial.Child.zeroMaximallySpecificMethods() : void contains an *invokespecial* instruction referencing a method invokespecial.SuperInterface.deletedBody() : void which doesn't have an implementation. This can lead to **AbstractMethodError** exception at runtime.
*/
public abstract class Child extends AbstractParent {
  public void bar() {
    super.foo();
  }

  public void zeroMaximallySpecificMethods() {
    super.deletedBody();
  }

  /*expected(PROBLEM)
 Attempt to execute instance instruction *invokespecial* on a static method invokespecial.AbstractParent.becomeStatic() : void

 Method mock.plugin.invokespecial.Child.invokeSpecialOnStaticMethod() : void contains an *invokespecial* instruction referencing a static method invokespecial.AbstractParent.becomeStatic() : void, what might have been caused by incompatible change of the method to static. This can lead to **IncompatibleClassChangeError** exception at runtime.
  */
  public void invokeSpecialOnStaticMethod() {
    super.becomeStatic();
  }
}
