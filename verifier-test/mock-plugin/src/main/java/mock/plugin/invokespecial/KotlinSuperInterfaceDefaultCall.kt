package mock.plugin.invokespecial

import invokespecial.InterfaceWithDefault

/**
 * @author Sergey Patrikeev
 */
class KotlinSuperInterfaceDefaultCall : InterfaceWithDefault {
  fun bar() {
    //contains invokespecial instruction to interface method
    //must not be reported as a problem
    @Suppress("DEFAULT_METHOD_CALL_FROM_JAVA6_TARGET", "DEFAULT_METHOD_CALL_FROM_JAVA6_TARGET_ERROR")
    super.foo()
  }
}