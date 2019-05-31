package mock.plugin.nonExtendable;

import nonExtendable.NonExtendableInterface;


/*expected(NON_EXTENDABLE)
Non-extendable interface 'nonExtendable.NonExtendableInterface' is inherited

Non-extendable interface 'nonExtendable.NonExtendableInterface' is inherited by 'mock.plugin.nonExtendable.NonExtendableAnonynousInheritors$2'. This interface is marked with '@org.jetbrains.annotations.ApiStatus.NonExtendable', which indicates that the interface is not supposed to be extended. See documentation of the '@ApiStatus.NonExtendable' for more info.
*/
public class NonExtendableInterfaceImplementor implements NonExtendableInterface {
}
