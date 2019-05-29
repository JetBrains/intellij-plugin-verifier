package mock.plugin;

import removedClasses.RemovedInterface;

/*expected(PROBLEM)
Access to unresolved class removedClasses.RemovedInterface

Interface mock.plugin.NotFoundInterface references an unresolved class removedClasses.RemovedInterface. This can lead to **NoSuchClassError** exception at runtime.
 */
public interface NotFoundInterface extends RemovedInterface {
}
