package mock.plugin.overrideOnly;

import overrideOnly.AllOverrideOnlyMethodClass;

public class AllowedCallOfSuperConstructor extends AllOverrideOnlyMethodClass {
  //Implicit invocation of super constructor, which is implicitly marked with @OverrideOnly annotation on containing class declaration.
  //We must not report OverrideOnly violation in this case (MP-2666).
}
