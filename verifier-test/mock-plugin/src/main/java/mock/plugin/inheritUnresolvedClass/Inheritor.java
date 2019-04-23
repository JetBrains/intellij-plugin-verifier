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
public class Inheritor extends UnresolvedMethodImplementor implements AbstractMethodsHolder {
}
