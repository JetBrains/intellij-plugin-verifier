package mock.plugin.inheritUnresolvedClass;

import inheritUnresolvedClass.AbstractMethodsHolder;
import inheritUnresolvedClass.UnresolvedMethodImplementor;

/**
 * This class implements interface {@link AbstractMethodsHolder}
 * and inherits implementation of its abstract methods from {@link UnresolvedMethodImplementor}.
 * <p>
 * The {@link UnresolvedMethodImplementor} will be removed in new IDE
 * so we must not report problems "Method X from AbstractMethodHolder is not implemented".
 */

/*expected(PROBLEM)
  Access to unresolved class inheritUnresolvedClass.UnresolvedMethodImplementor

  Class mock.plugin.inheritUnresolvedClass.Inheritor references an unresolved class inheritUnresolvedClass.UnresolvedMethodImplementor. This can lead to **NoSuchClassError** exception at runtime.
*/

/*expected(PROBLEM)
  Access to unresolved class inheritUnresolvedClass.UnresolvedMethodImplementor

  Constructor mock.plugin.inheritUnresolvedClass.Inheritor.<init>() references an unresolved class inheritUnresolvedClass.UnresolvedMethodImplementor. This can lead to **NoSuchClassError** exception at runtime.
*/
public class Inheritor extends UnresolvedMethodImplementor implements AbstractMethodsHolder {
}
