package mock.plugin.access;

import access.AccessProblemBase;
import access.other.BecamePackagePrivate;

public class IllegalAccess extends AccessProblemBase {
  public void main(String[] args) {
    AccessProblemBase problem = new AccessProblemBase();

    //will be protected. and the inheritance of this class(IllegalAccess) from AccessProblemBase doesn't matter.
    foo();
    problem.foo();
    int x = problem.x;
  }

  public void classBecamePackagePrivate() {
    new BecamePackagePrivate();
  }
}
