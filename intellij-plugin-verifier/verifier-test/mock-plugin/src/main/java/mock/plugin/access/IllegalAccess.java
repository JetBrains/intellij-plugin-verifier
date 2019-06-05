package mock.plugin.access;

import access.AccessProblemBase;
import access.other.BecamePackagePrivate;

public class IllegalAccess extends AccessProblemBase {
  public void main(String[] args) {
    //This is allowed.
    foo();

    /*expected(PROBLEM)
    Illegal invocation of protected method access.AccessProblemBase.foo() : void

    Method mock.plugin.access.IllegalAccess.main(String[]) : void contains an *invokevirtual* instruction referencing a protected method access.AccessProblemBase.foo() : void inaccessible to a class mock.plugin.access.IllegalAccess. This can lead to **IllegalAccessError** exception at runtime.
     */

    AccessProblemBase problem = new AccessProblemBase();
    problem.foo();

    /*expected(PROBLEM)
    Illegal access to a protected field access.AccessProblemBase.x : int

    Method mock.plugin.access.IllegalAccess.main(String[]) : void contains a *getfield* instruction referencing a protected field access.AccessProblemBase.x : int inaccessible to a class mock.plugin.access.IllegalAccess. This can lead to **IllegalAccessError** exception at runtime.
     */
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
