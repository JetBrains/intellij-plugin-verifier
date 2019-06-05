package protectedAccess;

public class ProtectedMembersOwnerAccessInSamePackage {
  public void legalStaticAccess() {
    String staticField = ProtectedMembersOwner.staticField;
    ProtectedMembersOwner.staticMethod();
  }

  public void legalInstanceAccess(ProtectedMembersOwner owner) {
    String objectField = owner.objectField;
    owner.instanceMethod();
  }
}
