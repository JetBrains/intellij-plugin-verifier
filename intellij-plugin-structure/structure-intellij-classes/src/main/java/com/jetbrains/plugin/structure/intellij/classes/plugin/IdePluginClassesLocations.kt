package com.jetbrains.plugin.structure.intellij.classes.plugin

import com.jetbrains.plugin.structure.base.utils.closeLogged
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
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
      locations.values.forEach { it.closeLogged() }
      allocatedResource.closeLogged()
    }
  }

  fun getResolver(key: LocationKey): Resolver? = locations[key]

}
