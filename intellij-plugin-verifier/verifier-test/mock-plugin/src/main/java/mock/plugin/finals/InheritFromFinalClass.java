package mock.plugin.finals;

import finals.BecomeFinal;

/*expected(PROBLEM)
  Inheritance from a final class finals.BecomeFinal

  Class mock.plugin.finals.InheritFromFinalClass inherits from a final class finals.BecomeFinal. This can lead to **VerifyError** exception at runtime.
*/
public class InheritFromFinalClass extends BecomeFinal {
}
