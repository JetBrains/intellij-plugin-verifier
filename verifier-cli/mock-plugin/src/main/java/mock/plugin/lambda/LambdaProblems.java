package mock.plugin.lambda;

import invocation.InvocationProblems;

/**
 * @author Sergey Patrikeev
 */
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