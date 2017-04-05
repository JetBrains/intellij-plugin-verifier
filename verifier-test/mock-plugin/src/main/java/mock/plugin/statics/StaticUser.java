package mock.plugin.statics;

import statics.Derived;

/**
 * @author Sergey Patrikeev
 */
public class StaticUser {
  public static void main() {
    //no problems of moving the static method upward in the hierarchy
    Derived.method();
  }
}
