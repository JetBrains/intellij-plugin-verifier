package mock.plugin.inheritance;

import misc.BecomeClass;

/*expected(PROBLEM)
  Incompatible change of super interface misc.BecomeClass to class

  Interface mock.plugin.inheritance.SuperInterfaceBecomeClass has a *super interface* misc.BecomeClass which is actually a *class*. This can lead to **IncompatibleClassChangeError** exception at runtime.
*/
public interface SuperInterfaceBecomeClass extends BecomeClass {
}
