package defaults;

/**
 * @author Sergey Patrikeev
 */
public interface IfaceDefault extends Iface {
  @Override
  default void method() {
    //implementation
  }
}
