package com.jetbrains.plugin.structure.intellij.classes.locator

import com.jetbrains.plugin.structure.base.utils.closeLogged
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.classes.resolvers.UnionResolver
import java.io.Closeable

/**
 * @author Sergey Patrikeev
 */
data class ClassLocationsContainer(private val allocatedResource: Closeable,
                                   private val classesLocations: Map<LocationKey, Resolver>) : Closeable {

  private var isClosed: Boolean = false

  @Synchronized
  override fun close() {
    if (!isClosed) {
      isClosed = true
      allocatedResource.closeLogged()
      classesLocations.values.forEach { it.closeLogged() }
    }
  }

  fun getResolver(key: LocationKey): Resolver? = classesLocations[key]

  fun geyAllKeys(): Set<LocationKey> = classesLocations.keys

  fun getUnitedResolver() = UnionResolver.create(classesLocations.values)

}
