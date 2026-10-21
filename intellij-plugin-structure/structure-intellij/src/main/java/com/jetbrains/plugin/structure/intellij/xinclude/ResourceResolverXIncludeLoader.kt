/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.xinclude

import com.intellij.platform.pluginSystem.parser.impl.LoadedXIncludeReference
import com.intellij.platform.pluginSystem.parser.impl.XIncludeLoader
import com.jetbrains.plugin.structure.intellij.resources.ResourceResolver
import org.slf4j.LoggerFactory
import java.nio.file.Path

private val LOG = LoggerFactory.getLogger(ResourceResolverXIncludeLoader::class.java)

/**
 * Bridges the platform parser library's [XIncludeLoader] contract onto the [ResourceResolver] chain
 * this module already builds, so that `<xi:include>` resolution on the platform-parser path searches
 * the same resources as [XIncluder] does on the JAXB path (minus [XIncluder]'s own fallback lookups,
 * see below).
 *
 * ### Why the two contracts line up
 *
 * [path] arrives ALREADY joined by the library, into what [XIncludeLoader.loadXIncludeReference]'s own
 * documentation calls an "absolute path from a resource root, without leading `/`" - the library's
 * `LoadPathUtil.toLoadPath`/`getChildBaseDir` prepend the enclosing document's include base
 * (defaulting to `META-INF`) to a relative `href`, strip a leading `/` from an absolute one, and pass
 * V2 module references (`intellij.*`, `fleet.*`, `kotlin.*`) through untouched. So `path` is already
 * root-relative, and no further joining is wanted.
 *
 * [ResourceResolver], by contrast, joins sibling-relative: it computes
 * `basePath.resolveSibling(relativePath)`. Anchoring [basePath] at a dummy file directly inside
 * [resourceRoot] makes the two agree exactly, because
 * `resourceRoot.resolve(ANCHOR).resolveSibling(path) == resourceRoot/path`.
 *
 * That equivalence lets this class reuse the chain rather than re-implement lookup. For a plugin
 * loaded from a `lib` directory, the chain is
 * `CompositeResourceResolver(JarsResourceResolver(lib jars) + caller's resolver)`, so an include whose
 * target lives in a *different* JAR of `lib` resolves too.
 *
 * The fallback wrappers [XIncluder] adds around the resolver ([MetaInfResourceResolver],
 * [InParentPathResourceResolver]) are internal to [XIncluder] and NOT part of the chain handed to
 * `PluginCreator`, so no `META-INF/` segment is re-appended behind this class's back.
 */
internal class ResourceResolverXIncludeLoader(
  private val resourceResolver: ResourceResolver,
  private val resourceRoot: Path
) : XIncludeLoader {

  override fun loadXIncludeReference(path: String): LoadedXIncludeReference? {
    val basePath = resourceRoot.resolve(ANCHOR)
    return when (val result = resourceResolver.resolveResource(path, basePath)) {
      is ResourceResolver.Result.Found -> result.use {
        LoadedXIncludeReference(it.resourceStream.readBytes(), it.description)
      }
      // The library decides what a missing include means: it consults `<xi:fallback>` and
      // `PluginDescriptorReaderContext.isMissingIncludeIgnored` before failing.
      is ResourceResolver.Result.NotFound -> null.also {
        LOG.debug("Include '{}' not found under resource root [{}]", path, resourceRoot)
      }
      is ResourceResolver.Result.Failed -> throw result.exception
    }
  }

  override fun toString() = "resource root [$resourceRoot] via $resourceResolver"

  private companion object {
    /**
     * Stands in for "some file directly inside the resource root". Only its position matters:
     * [ResourceResolver] never looks the anchor itself up, only resolves siblings of it.
     */
    const val ANCHOR = "__xinclude_anchor__"
  }
}
