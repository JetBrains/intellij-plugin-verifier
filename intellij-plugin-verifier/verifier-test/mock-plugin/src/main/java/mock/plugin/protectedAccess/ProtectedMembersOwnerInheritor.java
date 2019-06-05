package mock.plugin.protectedAccess;

import protectedAccess.ProtectedMembersOwner;

public class ProtectedMembersOwnerInheritor extends ProtectedMembersOwner {

  public void illegalInstanceAccess(ProtectedMembersOwner owner) {
    /*expected(PROBLEM)
    Illegal access to a protected field protectedAccess.ProtectedMembersOwner.objectField : String

    Method mock.plugin.protectedAccess.ProtectedMembersOwnerInheritor.illegalInstanceAccess(ProtectedMembersOwner) : void contains a *getfield* instruction referencing a protected field protectedAccess.ProtectedMembersOwner.objectField : java.lang.String inaccessible to a class mock.plugin.protectedAccess.ProtectedMembersOwnerInheritor. This can lead to **IllegalAccessError** exception at runtime.
    */
    String illegalProtectedFieldAccess = owner.objectField;

     /*expected(PROBLEM)
    Illegal invocation of protected method protectedAccess.ProtectedMembersOwner.instanceMethod() : void

    Method mock.plugin.protectedAccess.ProtectedMembersOwnerInheritor.illegalInstanceAccess(ProtectedMembersOwner) : void contains an *invokevirtual* instruction referencing a protected method protectedAccess.ProtectedMembersOwner.instanceMethod() : void inaccessible to a class mock.plugin.protectedAccess.ProtectedMembersOwnerInheritor. This can lead to **IllegalAccessError** exception at runtime.
    */
    owner.instanceMethod();
  }

  public void legalInstanceAccess(ProtectedMembersOwnerInheritor inheritor) {
    //These are legal because accesses are via inheritor.
    String legalProtectedFieldAccess = inheritor.objectField;
    inheritor.instanceMethod();
  }

  public void legalStaticAccess() {
    String legalStaticAccess = ProtectedMembersOwner.staticField;
    ProtectedMembersOwner.staticMethod();
  }

}
