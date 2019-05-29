package mock.plugin.access;

import access.AccessProblemDerived;

public class VirtualAccess {
  /*expected(PROBLEM)
  Illegal invocation of protected method access.AccessProblemBase.foo() : void

  Method mock.plugin.access.VirtualAccess.virtualMethodBecameProtected() : void contains an *invokevirtual* instruction referencing access.AccessProblemDerived.foo() : void which is resolved to a protected method access.AccessProblemBase.foo() : void inaccessible to a class mock.plugin.access.VirtualAccess. This can lead to **IllegalAccessError** exception at runtime.
   */
  public void virtualMethodBecameProtected() {
    //foo() will be protected
    new AccessProblemDerived().foo();
  }

  /*expected(PROBLEM)
  Illegal access to a protected field access.AccessProblemBase.x : int

  Method mock.plugin.access.VirtualAccess.inheritedFieldBecameProtected() : void contains a *getfield* instruction referencing access.AccessProblemDerived.x : int which is resolved to a protected field access.AccessProblemBase.x : int inaccessible to a class mock.plugin.access.VirtualAccess. This can lead to **IllegalAccessError** exception at runtime.
   */
  public void inheritedFieldBecameProtected() {
    //foo() will be protected
    int x = new AccessProblemDerived().x;
  }

}
