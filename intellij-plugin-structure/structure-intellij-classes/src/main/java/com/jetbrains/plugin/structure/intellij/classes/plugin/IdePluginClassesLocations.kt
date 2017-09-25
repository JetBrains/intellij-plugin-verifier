package com.jetbrains.plugin.structure.intellij.classes.plugin

import com.jetbrains.plugin.structure.base.utils.closeLogged
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.classes.resolvers.UnionResolver
import com.jetbrains.plugin.structure.intellij.classes.locator.LocationKey
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import java.io.Closeable

/**
 * @author Sergey Patrikeev
 */
data class IdePluginClassesLocations(val idePlugin: IdePlugin,
                                     private val allocatedResource: Closeable,
                                     private val locations: Map<LocationKey, Resolver>) : Closeable {

  private var isClosed: Boolean = false

  @Synchronized
  override fun close() {
    if (!isClosed) {
      isClosed = true
      allocatedResource.closeLogged()
      locations.values.forEach { it.closeLogged() }
    }
  }

  fun getResolver(key: LocationKey): Resolver? = locations[key]

  fun getAllKeys(): Set<LocationKey> = locations.keys

  fun getUnitedResolver(): Resolver = UnionResolver.create(locations.values)

}
