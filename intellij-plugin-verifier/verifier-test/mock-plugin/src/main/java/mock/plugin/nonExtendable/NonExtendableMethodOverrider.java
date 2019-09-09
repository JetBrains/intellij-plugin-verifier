package mock.plugin.nonExtendable;

import nonExtendable.NonExtendableMethodOwner;

/*expected(NON_EXTENDABLE)
Non-extendable method nonExtendable.NonExtendableMethodOwner.nonExtendableMethod() is overridden

Non-extendable method nonExtendable.NonExtendableMethodOwner.nonExtendableMethod() : void is overridden by mock.plugin.nonExtendable.NonExtendableMethodOverrider.nonExtendableMethod() : void. This method is marked with @org.jetbrains.annotations.ApiStatus.NonExtendable annotation, which indicates that the method is not supposed to be overridden by client code. See documentation of the @ApiStatus.NonExtendable for more info.
*/
public class NonExtendableMethodOverrider extends NonExtendableMethodOwner {
  @Override
  public void nonExtendableMethod() {
    super.nonExtendableMethod();
  }
}
