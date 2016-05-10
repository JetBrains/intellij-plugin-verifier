package mock.plugin.invokeStaticOnInstance;

import invocation.InvocationProblems;

/**
 * Created by Sergey Patrikeev
 */
public class InvocationProblemsUser {
  public void foo() {
    InvocationProblems.wasStatic();
  }
}
