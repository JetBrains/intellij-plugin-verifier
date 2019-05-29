package mock.plugin.removedClasses;

import removedClasses.RemovedClass;

/*expected(PROBLEM)
Access to unresolved class removedClasses.RemovedClass

Constructor mock.plugin.removedClasses.UsesRemoved.<init>() references an unresolved class removedClasses.RemovedClass. This can lead to **NoSuchClassError** exception at runtime.
*/

/*expected(PROBLEM)
Access to unresolved class removedClasses.RemovedClass

Class mock.plugin.removedClasses.UsesRemoved references an unresolved class removedClasses.RemovedClass. This can lead to **NoSuchClassError** exception at runtime.
*/
public class UsesRemoved extends RemovedClass {
}
