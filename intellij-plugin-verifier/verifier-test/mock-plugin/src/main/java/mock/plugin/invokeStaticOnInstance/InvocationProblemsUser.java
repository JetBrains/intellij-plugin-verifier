package mock.plugin.invokeStaticOnInstance;

import invocation.InvocationProblems;

public class InvocationProblemsUser {

  /*expected(PROBLEM)
  Attempt to execute *invokestatic* instruction on instance method invocation.InvocationProblems.wasStatic() : void

  Method mock.plugin.invokeStaticOnInstance.InvocationProblemsUser.foo() : void contains *invokestatic* instruction referencing instance method invocation.InvocationProblems.wasStatic() : void, what might have been caused by incompatible change of the method from static to instance. This can lead to **IncompatibleClassChangeError** exception at runtime.
  */
  public void foo() {
    InvocationProblems.wasStatic();
  }
}
