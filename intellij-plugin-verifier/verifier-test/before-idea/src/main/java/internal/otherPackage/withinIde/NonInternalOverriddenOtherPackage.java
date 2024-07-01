package internal.otherPackage.withinIde;

import internal.noWarning.InternalInterface;

public class NonInternalOverriddenOtherPackage implements InternalInterface {
    // This method overrides an internal method from other package, but it is not internal itself.
    @Override
    public void someMethod() {
    }
}