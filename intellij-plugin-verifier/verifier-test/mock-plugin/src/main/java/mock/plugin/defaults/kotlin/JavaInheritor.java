package mock.plugin.defaults.kotlin;

import defaults.kotlin.I;

/*expected(PROBLEM)
  Abstract method defaults.kotlin.I.noDefault() : int is not implemented

  Concrete class mock.plugin.defaults.kotlin.JavaInheritor inherits from defaults.kotlin.I but doesn't implement the abstract method noDefault() : int. This can lead to **AbstractMethodError** exception at runtime.
*/
public class JavaInheritor implements I {

}
