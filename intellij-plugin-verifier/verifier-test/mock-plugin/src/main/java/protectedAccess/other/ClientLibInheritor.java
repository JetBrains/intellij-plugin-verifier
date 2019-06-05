package protectedAccess.other;

import protectedAccess.ProtectedMembersOwner;

public class ClientLibInheritor extends ProtectedMembersOwner {
  public void illegalUsages() {
    //These accesses are illegal because LibInheritor will not have overriding of 'instanceMethod' and 'objectField'
    // in thew "new" version. But they are 'protected' in the new version. And this 'ClientLibInheritor' class
    // is not inheritor of LibInheritor but of ProtectedMembersOwner.

    // This example is found in article "Checking Access to Protected Members in the Java Virtual Machine" by Alessandro Coglio, 2005
    // example in Figure.3

    LibInheritor libInheritor = new LibInheritor();

    /*expected(PROBLEM)
    Illegal access to a protected field protectedAccess.ProtectedMembersOwner.objectField : String

    Method protectedAccess.other.ClientLibInheritor.illegalUsages() : void contains a *getfield* instruction referencing protectedAccess.other.LibInheritor.objectField : java.lang.String which is resolved to a protected field protectedAccess.ProtectedMembersOwner.objectField : java.lang.String inaccessible to a class protectedAccess.other.ClientLibInheritor. This can lead to **IllegalAccessError** exception at runtime.
     */
    String objectField = libInheritor.objectField;

    /*expected(PROBLEM)
    Illegal invocation of protected method protectedAccess.ProtectedMembersOwner.instanceMethod() : void

    Method protectedAccess.other.ClientLibInheritor.illegalUsages() : void contains an *invokevirtual* instruction referencing protectedAccess.other.LibInheritor.instanceMethod() : void which is resolved to a protected method protectedAccess.ProtectedMembersOwner.instanceMethod() : void inaccessible to a class protectedAccess.other.ClientLibInheritor. This can lead to **IllegalAccessError** exception at runtime.
     */
    libInheritor.instanceMethod();
  }
}
