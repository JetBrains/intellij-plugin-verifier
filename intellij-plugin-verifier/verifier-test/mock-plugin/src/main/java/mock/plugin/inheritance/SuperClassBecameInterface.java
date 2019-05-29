package mock.plugin.inheritance;

import misc.BecomeInterface;

/*expected(PROBLEM)
Incompatible change of super class misc.BecomeInterface to interface

Class mock.plugin.inheritance.SuperClassBecameInterface has a *super class* misc.BecomeInterface which is actually an *interface*. This can lead to **IncompatibleClassChangeError** at runtime.
*/

/*expected(PROBLEM)
Invocation of unresolved constructor misc.BecomeInterface.<init>()

Constructor mock.plugin.inheritance.SuperClassBecameInterface.<init>() contains an *invokespecial* instruction referencing an unresolved constructor misc.BecomeInterface.<init>(). This can lead to **NoSuchMethodError** exception at runtime.
*/
public class SuperClassBecameInterface extends BecomeInterface {
}
