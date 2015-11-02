package mock.plugin;

/**
 * @author Sergey Patrikeev
 */
public class NoProblemInvocation extends OverrideFinalMethodProblem {
  public void someMethod() {
    //no problems (invocation of protected method)
    actionPerformed(null);
  }
}
