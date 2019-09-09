package mock.plugin.nonExtendable;

import nonExtendable.NonExtendableClass;


/*expected(NON_EXTENDABLE)
Non-extendable class nonExtendable.NonExtendableClass is extended

Non-extendable class nonExtendable.NonExtendableClass is extended by mock.plugin.nonExtendable.NonExtendableClassInheritor. This class is marked with @org.jetbrains.annotations.ApiStatus.NonExtendable, which indicates that the class is not supposed to be extended. See documentation of the @ApiStatus.NonExtendable for more info.
*/
public class NonExtendableClassInheritor extends NonExtendableClass {
}
