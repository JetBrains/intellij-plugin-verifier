package mock.plugin.invokeStaticOnInstance;

import invocation.InvocationProblems;

public class InvocationProblemsUser {
  public void foo() {
    InvocationProblems.wasStatic();
  }
}
