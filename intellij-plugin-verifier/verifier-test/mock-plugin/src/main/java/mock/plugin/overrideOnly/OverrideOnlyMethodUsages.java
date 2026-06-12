package mock.plugin.overrideOnly;

import overrideOnly.AllOverrideOnlyMethodsOwner;
import overrideOnly.OverrideOnlyJavaSingleton;
import overrideOnly.OverrideOnlyKotlinCompanionHolder;
import overrideOnly.OverrideOnlyKotlinInterfaceWithCompanion;
import overrideOnly.OverrideOnlyKotlinObject;
import overrideOnly.OverrideOnlyMethodOwner;

import java.util.function.Consumer;

public class OverrideOnlyMethodUsages {
  public void usages(OverrideOnlyMethodOwner owner1, AllOverrideOnlyMethodsOwner owner2) {
    /*expected(OVERRIDE_ONLY)
    Invocation of override-only method overrideOnly.OverrideOnlyMethodOwner.overrideOnlyMethod()

    Override-only method overrideOnly.OverrideOnlyMethodOwner.overrideOnlyMethod() is invoked in mock.plugin.overrideOnly.OverrideOnlyMethodUsages.usages(OverrideOnlyMethodOwner, AllOverrideOnlyMethodsOwner) : void. This method is marked with @org.jetbrains.annotations.ApiStatus.OverrideOnly annotation, which indicates that the method must be only overridden but not invoked by client code. See documentation of the @ApiStatus.OverrideOnly for more info.
    */

    owner1.overrideOnlyMethod();

    /*expected(OVERRIDE_ONLY)
    Invocation of override-only method overrideOnly.AllOverrideOnlyMethodsOwner.overrideOnlyMethod()

    Override-only method overrideOnly.AllOverrideOnlyMethodsOwner.overrideOnlyMethod() is invoked in mock.plugin.overrideOnly.OverrideOnlyMethodUsages.usages(OverrideOnlyMethodOwner, AllOverrideOnlyMethodsOwner) : void. This method is marked with @org.jetbrains.annotations.ApiStatus.OverrideOnly annotation, which indicates that the method must be only overridden but not invoked by client code. See documentation of the @ApiStatus.OverrideOnly for more info.
    */
    owner2.overrideOnlyMethod();

    // Calls to a static @OverrideOnly target must not be flagged: static dispatch
    // cannot be overridden, so the annotation has no enforceable meaning here.
    OverrideOnlyMethodOwner.staticOverrideOnlyMethod();

    // Methods on a Kotlin `object` or `companion object` are not overridable by
    // callers, so @OverrideOnly is unenforceable and these calls must not be flagged.
    OverrideOnlyKotlinObject.INSTANCE.overrideOnlyMethod();
    OverrideOnlyKotlinCompanionHolder.Companion.overrideOnlyMethod();

    // A hand-written Java singleton has the same shape as a Kotlin `object`: a final
    // class reached through a static INSTANCE field, so the method cannot be overridden
    // by callers and @OverrideOnly is unenforceable. This call must not be flagged.
    OverrideOnlyJavaSingleton.INSTANCE.overrideOnlyMethod();

    // Real-world case: an @OverrideOnly interface with a companion-object factory.
    // The factory call must not be flagged even though the interface carries the
    // annotation: the companion is a singleton, so its methods are not overridable.
    OverrideOnlyKotlinInterfaceWithCompanion instance = OverrideOnlyKotlinInterfaceWithCompanion.Companion.create();

    /*expected(OVERRIDE_ONLY)
    Invocation of override-only method overrideOnly.OverrideOnlyKotlinInterfaceWithCompanion.isSatisfied()

    Override-only method overrideOnly.OverrideOnlyKotlinInterfaceWithCompanion.isSatisfied() is invoked in mock.plugin.overrideOnly.OverrideOnlyMethodUsages.usages(OverrideOnlyMethodOwner, AllOverrideOnlyMethodsOwner) : void. This method is marked with @org.jetbrains.annotations.ApiStatus.OverrideOnly annotation, which indicates that the method must be only overridden but not invoked by client code. See documentation of the @ApiStatus.OverrideOnly for more info.
    */
    instance.isSatisfied();
  }

  public static void methodReferences() {
    /*expected(OVERRIDE_ONLY)
    Invocation of override-only method overrideOnly.OverrideOnlyMethodOwner.overrideOnlyMethod()

    Override-only method overrideOnly.OverrideOnlyMethodOwner.overrideOnlyMethod() is invoked in mock.plugin.overrideOnly.OverrideOnlyMethodUsages.methodReferences() : void. This method is marked with @org.jetbrains.annotations.ApiStatus.OverrideOnly annotation, which indicates that the method must be only overridden but not invoked by client code. See documentation of the @ApiStatus.OverrideOnly for more info.
    */
    Consumer<OverrideOnlyMethodOwner> overrideOnlyMethod = OverrideOnlyMethodOwner::overrideOnlyMethod;

    /*expected(OVERRIDE_ONLY)
    Invocation of override-only method overrideOnly.AllOverrideOnlyMethodsOwner.overrideOnlyMethod()

    Override-only method overrideOnly.AllOverrideOnlyMethodsOwner.overrideOnlyMethod() is invoked in mock.plugin.overrideOnly.OverrideOnlyMethodUsages.methodReferences() : void. This method is marked with @org.jetbrains.annotations.ApiStatus.OverrideOnly annotation, which indicates that the method must be only overridden but not invoked by client code. See documentation of the @ApiStatus.OverrideOnly for more info.
    */
    Consumer<AllOverrideOnlyMethodsOwner> allOverrideOnlyMethod = AllOverrideOnlyMethodsOwner::overrideOnlyMethod;
  }

}
