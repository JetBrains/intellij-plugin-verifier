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

  /*expected(PROBLEM)
  Illegal access to package-private class access.other.BecamePackagePrivate

  Package-private class access.other.BecamePackagePrivate is not available at mock.plugin.access.IllegalAccess.classBecamePackagePrivate() : void. This can lead to **IllegalAccessError** exception at runtime.
  */

  /*expected(PROBLEM)
  Illegal invocation of package-private constructor access.other.BecamePackagePrivate.<init>()

  Method mock.plugin.access.IllegalAccess.classBecamePackagePrivate() : void contains an *invokespecial* instruction referencing a package-private constructor access.other.BecamePackagePrivate.<init>() inaccessible to a class mock.plugin.access.IllegalAccess. This can lead to **IllegalAccessError** exception at runtime.
   */
  public void classBecamePackagePrivate() {
    new BecamePackagePrivate();
  }
}
