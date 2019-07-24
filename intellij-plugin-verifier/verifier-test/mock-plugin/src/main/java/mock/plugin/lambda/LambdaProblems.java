package mock.plugin.lambda;

import invocation.InvocationProblems;

public class LambdaProblems {

  /*expected(PROBLEM)
    Invocation of unresolved method invocation.InvocationProblems.deleted() : void

    Method mock.plugin.lambda.LambdaProblems.invokeDeletedFromLambda() : void contains an *invokevirtual* instruction referencing an unresolved method invocation.InvocationProblems.deleted() : void. This can lead to **NoSuchMethodError** exception at runtime.
     */
  public void invokeDeletedFromLambda() {
    InvocationProblems problems = new InvocationProblems();
    bar(problems::deleted);
  }

  public void bar(C c) {

  }

  interface C {
    void foo();
  }

}