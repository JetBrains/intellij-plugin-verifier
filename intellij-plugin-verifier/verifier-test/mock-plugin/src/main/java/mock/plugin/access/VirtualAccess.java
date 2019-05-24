package mock.plugin.access;

import access.AccessProblemDerived;

public class VirtualAccess {
  public void virtualMethodBecameProtected() {
    //foo() will be protected
    new AccessProblemDerived().foo();
  }

  public void inheritedFieldBecameProtected() {
    //foo() will be protected
    int x = new AccessProblemDerived().x;
  }

}
