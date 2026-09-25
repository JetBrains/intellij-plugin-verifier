/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

import com.intellij.openapi.diagnostic.Logger
import org.slf4j.LoggerFactory

private val LOG = LoggerFactory.getLogger("com.jetbrains.plugin.structure.intellij.plugin.PlatformParserLogging")

/**
 * Routes only the platform parser library's own `com.intellij.openapi.diagnostic.Logger` category to
 * SLF4J, so that the diagnostics it logs stay diagnostics instead of failing the plugin being parsed.
 *
 * ### Why this is needed
 *
 * The library reports several kinds of malformed-but-recoverable descriptor content through
 * `Logger.error(...)` and then *carries on building the element anyway* - an unrecognised
 * `<extensionPoint area="...">` value (only `IDEA_PROJECT` and `IDEA_MODULE` are recognised, so the
 * perfectly ordinary legacy `IDEA_APPLICATION` lands here), an `<action>` with neither `id` nor `class`,
 * a `<listener>` missing attributes, an unknown `client` value on a service, and any `<idea-plugin>`
 * child element this parser build has no support for (`<icon>`, for instance). Inside an IDE, where a
 * real `Logger` implementation is installed, all of those are log entries and the descriptor still
 * loads.
 *
 * Outside an IDE there is no implementation installed, and `Logger`'s default one throws
 * `AssertionError` from `error(...)`. That turned every one of those cases into a failed plugin - which
 * is not the library's behaviour, just an artefact of hosting it in a plain JVM. Installing a logging
 * implementation for that category restores the intended semantics.
 *
 * ### Why it is guarded
 *
 * `Logger.setFactory` is process-global, and `structure-intellij` is a published library that may well
 * be embedded in something that has its own opinion - an IDE, most obviously, but also any host that
 * installs a factory of its own. So the factory is installed only when nothing has claimed it yet, and
 * only once. The installed factory delegates every category except the parser's back to the previous
 * factory, preserving the semantics of JPS and all other IntelliJ utility code. A host that has
 * already set a factory keeps it, and one that sets it later wins outright.
 */
internal object PlatformParserLogging {
  private const val PLATFORM_PARSER_LOGGER_CATEGORY = "com.intellij.platform.pluginSystem.parser.impl.PluginParser"

  @Volatile
  private var installed = false

  /**
   * Installs the bridge if no other `Logger.Factory` is in place. Idempotent and safe to call from any
   * thread; cheap enough to call before every parse.
   */
  @Synchronized
  fun install() {
    if (installed) return

    if (Logger.isInitialized()) {
      LOG.debug("A com.intellij.openapi.diagnostic.Logger factory is already installed; leaving it in place")
    } else {
      val delegate = Logger.getFactory()
      Logger.setFactory(Logger.Factory { category ->
        if (category.removePrefix("#") == PLATFORM_PARSER_LOGGER_CATEGORY) {
          Slf4jBackedLogger(LoggerFactory.getLogger(category))
        } else {
          delegate.getLoggerInstance(category)
        }
      })
    }
    installed = true
  }

  /**
   * Note `error` logs rather than throws - that is the entire point of this class. It is logged at WARN,
   * not ERROR: from plugin-verifier's perspective these are properties of the third-party descriptor
   * under inspection, not faults of the verifier, and a corpus run would otherwise emit them by the
   * thousand at ERROR.
   */
  private class Slf4jBackedLogger(private val delegate: org.slf4j.Logger) : Logger() {
    override fun isDebugEnabled() = delegate.isDebugEnabled

    override fun debug(message: String?, t: Throwable?) = delegate.debug(message, t)

    override fun info(message: String?, t: Throwable?) = delegate.info(message, t)

    override fun warn(message: String?, t: Throwable?) = delegate.warn(message, t)

    override fun error(message: String?, t: Throwable?, vararg details: String) {
      if (details.isEmpty()) {
        delegate.warn(message, t)
      } else {
        delegate.warn("$message ${details.joinToString(separator = ", ")}", t)
      }
    }
  }
}
