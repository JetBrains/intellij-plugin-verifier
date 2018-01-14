package mock.plugin.lambda;

import invocation.InvocationProblems;

public class LambdaProblems {

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