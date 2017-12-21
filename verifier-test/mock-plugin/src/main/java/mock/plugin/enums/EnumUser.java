package mock.plugin.enums;

/**
 * Conversion of a class with only public static final fields
 * to enum class doesn't lead to compatibility problems.
 */
public class EnumUser {
  public static final EnumUser FIELD = new EnumUser();
}