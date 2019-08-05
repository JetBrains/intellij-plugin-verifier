package mock.plugin.non.existing;

import invokevirtual.Child;

public class InvokeRemovedMethod {
  /*expected(PROBLEM)
  Invocation of unresolved method invokevirtual.Child.removedMethod() : void

  Method mock.plugin.non.existing.InvokeRemovedMethod.foo() : void contains an *invokevirtual* instruction referencing an unresolved method invokevirtual.Child.removedMethod() : void. This can lead to **NoSuchMethodError** exception at runtime.
  The method might have been declared in the super class: invokevirtual.Parent
   */
  public void foo() {
    Child child = new Child();
    child.removedMethod();
  }

  /*expected(PROBLEM)
  Invocation of unresolved method mock.plugin.non.existing.InheritMethod.removedMethod() : void

  Method mock.plugin.non.existing.InvokeRemovedMethod.invokeVirtual() : void contains an *invokevirtual* instruction referencing an unresolved method mock.plugin.non.existing.InheritMethod.removedMethod() : void. This can lead to **NoSuchMethodError** exception at runtime.
  The method might have been declared in the super class or in the super interfaces:
    invokevirtual.Parent
    interfaces.SomeInterface
    interfaces.SomeInterface2
   */
  public void invokeVirtual() {
    new InheritMethod().removedMethod();
  }

  /*expected(PROBLEM)
  Invocation of unresolved method mock.plugin.non.existing.InheritMethod.removedMethod() : void

  Method mock.plugin.non.existing.InvokeRemovedMethod.invokeStatic() : void contains an *invokevirtual* instruction referencing an unresolved method mock.plugin.non.existing.InheritMethod.removedMethod() : void. This can lead to **NoSuchMethodError** exception at runtime.
  The method might have been declared in the super class or in the super interfaces:
    invokevirtual.Parent
    interfaces.SomeInterface
    interfaces.SomeInterface2
   */
  public void invokeStatic() {
    new InheritMethod().removedMethod();
  }

}
