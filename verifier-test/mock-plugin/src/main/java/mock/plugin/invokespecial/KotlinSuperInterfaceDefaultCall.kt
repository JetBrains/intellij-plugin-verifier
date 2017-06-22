package mock.plugin.invokespecial

import invokespecial.InterfaceWithDefault

/**
 * @author Sergey Patrikeev
 */
class KotlinSuperInterfaceDefaultCall : InterfaceWithDefault {
  fun bar() {
    //contains invokespecial instruction to interface method
    //must not be reported as a problem
    super.foo()
  }
}