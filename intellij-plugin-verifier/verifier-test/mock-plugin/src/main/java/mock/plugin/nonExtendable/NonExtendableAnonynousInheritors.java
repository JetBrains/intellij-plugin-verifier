package mock.plugin.nonExtendable;

import nonExtendable.NonExtendableClass;
import nonExtendable.NonExtendableInterface;
import nonExtendable.NonExtendableMethodOwner;

public class NonExtendableAnonynousInheritors {
  public void badInheritors() {

    /*expected(NON_EXTENDABLE)
    Non-extendable class 'nonExtendable.NonExtendableClass' is extended

    Non-extendable class 'nonExtendable.NonExtendableClass' is extended by 'mock.plugin.nonExtendable.NonExtendableAnonynousInheritors$1'. This class is marked with '@org.jetbrains.annotations.ApiStatus.NonExtendable', which indicates that the class is not supposed to be extended. See documentation of the '@ApiStatus.NonExtendable' for more info.
    */
    NonExtendableClass badAnonymousClass = new NonExtendableClass() {
    };

    /*expected(NON_EXTENDABLE)
    Non-extendable interface 'nonExtendable.NonExtendableInterface' is implemented

    Non-extendable interface 'nonExtendable.NonExtendableInterface' is implemented by 'mock.plugin.nonExtendable.NonExtendableInterfaceImplementor'. This interface is marked with '@org.jetbrains.annotations.ApiStatus.NonExtendable', which indicates that the interface is not supposed to be extended. See documentation of the '@ApiStatus.NonExtendable' for more info.
    */
    NonExtendableInterface badAnonymousInterface = new NonExtendableInterface() {
    };

    /*expected(NON_EXTENDABLE)
    Non-extendable method 'nonExtendable.NonExtendableMethodOwner.nonExtendableMethod()' is overridden

    Non-extendable method 'nonExtendable.NonExtendableMethodOwner.nonExtendableMethod() : void' is overridden by 'mock.plugin.nonExtendable.NonExtendableAnonynousInheritors$3.nonExtendableMethod() : void'. This method is marked with '@org.jetbrains.annotations.ApiStatus.NonExtendable' annotation, which indicates that the method is not supposed to be overridden by client code. See documentation of the '@ApiStatus.NonExtendable' for more info.
    */
    NonExtendableMethodOwner badAnonymousMethodOverrider = new NonExtendableMethodOwner() {
      @Override
      public void nonExtendableMethod() {
        super.nonExtendableMethod();
      }
    };
  }
}
