package mock.plugin.lambda;

import invocation.InvocationProblems;

public class LambdaProblems {

  public static void main(String[] args) {
    new LambdaProblems().invokeVirtualOnStatic();
  }


  /*expected(PROBLEM)
    Invocation of unresolved method invocation.InvocationProblems.deleted() : void

    Method mock.plugin.lambda.LambdaProblems.invokeDeletedMethodFromLambda() : void contains an *invokevirtual* instruction referencing an unresolved method invocation.InvocationProblems.deleted() : void. This can lead to **NoSuchMethodError** exception at runtime.
  */
  public void invokeDeletedMethodFromLambda() {
    InvocationProblems problems = new InvocationProblems();
    bar(problems::deleted);
  }

  /*expected(PROBLEM)
    Illegal invocation of private method invocation.InvocationProblems.becamePrivate() : void

    Method mock.plugin.lambda.LambdaProblems.invokePrivateMethodFromLambda() : void contains an *invokevirtual* instruction referencing a private method invocation.InvocationProblems.becamePrivate() : void inaccessible to a class mock.plugin.lambda.LambdaProblems. This can lead to **IllegalAccessError** exception at runtime.
  */
  public void invokePrivateMethodFromLambda() {
    InvocationProblems problems = new InvocationProblems();
    bar(problems::becamePrivate);
  }

  /*expected(PROBLEM)
    Attempt to execute *invokestatic* instruction on instance method invocation.InvocationProblems.wasStatic() : void

    Method mock.plugin.lambda.LambdaProblems.invokeVirtualOnStatic() : void contains *invokestatic* instruction referencing instance method invocation.InvocationProblems.wasStatic() : void, what might have been caused by incompatible change of the method from static to instance. This can lead to **IncompatibleClassChangeError** exception at runtime.
   */
  public void invokeVirtualOnStatic() {
    Runnable wasStatic = InvocationProblems::wasStatic;
    wasStatic.run();
  }

  public void bar(C c) {

  }

  interface C {
    void foo();
  }

}