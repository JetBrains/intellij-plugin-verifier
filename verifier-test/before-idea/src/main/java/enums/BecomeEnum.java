package enums;

/**
 * Conversion of a class with only public static final fields
 * to enum class doesn't lead to compatibility problems.
 */
public class BecomeEnum {
  public static final BecomeEnum FIELD = new BecomeEnum();
}