package mock.plugin.noproblems.protected_inner;

import mock.plugin.OverrideFinalMethodProblem;

public class NoProblemInvocation extends OverrideFinalMethodProblem {
  public void someMethod() {
    //no problems (invocation of protected method)
    actionPerformed(null);
  }
}
