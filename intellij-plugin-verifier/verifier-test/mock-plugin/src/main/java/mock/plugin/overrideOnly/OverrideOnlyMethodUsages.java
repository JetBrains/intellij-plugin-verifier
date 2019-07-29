package mock.plugin.overrideOnly;

import overrideOnly.AllOverrideOnlyMethodsOwner;
import overrideOnly.OverrideOnlyMethodOwner;

import java.util.function.Consumer;

public class OverrideOnlyMethodUsages {
  public void usages(OverrideOnlyMethodOwner owner1, AllOverrideOnlyMethodsOwner owner2) {
    /*expected(OVERRIDE_ONLY)
    Invocation of override-only method 'overrideOnly.OverrideOnlyMethodOwner.overrideOnlyMethod()'

    Override-only method 'overrideOnly.OverrideOnlyMethodOwner.overrideOnlyMethod()' is invoked in 'mock.plugin.overrideOnly.OverrideOnlyMethodUsages.usages(OverrideOnlyMethodOwner, AllOverrideOnlyMethodsOwner) : void'. This method is marked with '@org.jetbrains.annotations.ApiStatus.OverrideOnly' annotation, which indicates that the method must be only overridden but not invoked by client code. See documentation of the '@ApiStatus.OverrideOnly' for more info.
    */

    owner1.overrideOnlyMethod();

    /*expected(OVERRIDE_ONLY)
    Invocation of override-only method 'overrideOnly.AllOverrideOnlyMethodsOwner.overrideOnlyMethod()'

    Override-only method 'overrideOnly.AllOverrideOnlyMethodsOwner.overrideOnlyMethod()' is invoked in 'mock.plugin.overrideOnly.OverrideOnlyMethodUsages.usages(OverrideOnlyMethodOwner, AllOverrideOnlyMethodsOwner) : void'. This method is marked with '@org.jetbrains.annotations.ApiStatus.OverrideOnly' annotation, which indicates that the method must be only overridden but not invoked by client code. See documentation of the '@ApiStatus.OverrideOnly' for more info.
    */
    owner2.overrideOnlyMethod();
  }

  public static void methodReferences() {
    /*expected(OVERRIDE_ONLY)
    Invocation of override-only method 'overrideOnly.OverrideOnlyMethodOwner.overrideOnlyMethod()'

    Override-only method 'overrideOnly.OverrideOnlyMethodOwner.overrideOnlyMethod()' is invoked in 'mock.plugin.overrideOnly.OverrideOnlyMethodUsages.methodReferences() : void'. This method is marked with '@org.jetbrains.annotations.ApiStatus.OverrideOnly' annotation, which indicates that the method must be only overridden but not invoked by client code. See documentation of the '@ApiStatus.OverrideOnly' for more info.
    */
    Consumer<OverrideOnlyMethodOwner> overrideOnlyMethod = OverrideOnlyMethodOwner::overrideOnlyMethod;

    /*expected(OVERRIDE_ONLY)
    Invocation of override-only method 'overrideOnly.AllOverrideOnlyMethodsOwner.overrideOnlyMethod()'

    Override-only method 'overrideOnly.AllOverrideOnlyMethodsOwner.overrideOnlyMethod()' is invoked in 'mock.plugin.overrideOnly.OverrideOnlyMethodUsages.methodReferences() : void'. This method is marked with '@org.jetbrains.annotations.ApiStatus.OverrideOnly' annotation, which indicates that the method must be only overridden but not invoked by client code. See documentation of the '@ApiStatus.OverrideOnly' for more info.
    */
    Consumer<AllOverrideOnlyMethodsOwner> allOverrideOnlyMethod = AllOverrideOnlyMethodsOwner::overrideOnlyMethod;
  }

}
