package com.jetbrains.plugin.structure.intellij.verifiers

import com.jetbrains.plugin.structure.base.problems.PropertyNotSpecified
import com.jetbrains.plugin.structure.intellij.beans.PluginBean
import com.jetbrains.plugin.structure.intellij.problems.HttpLinkInDescription
import com.jetbrains.plugin.structure.intellij.problems.MIN_DESCRIPTION_LENGTH
import com.jetbrains.plugin.structure.intellij.problems.PropertyWithDefaultValue
import com.jetbrains.plugin.structure.intellij.problems.ShortOrNonLatinDescription
import org.jsoup.Jsoup

private const val MAX_LONG_PROPERTY_LENGTH = 65535

private val DEFAULT_TEMPLATE_DESCRIPTIONS = setOf(
  "Enter short description for your plugin here", "most HTML tags may be used", "example.com/my-framework"
)

class PluginDescriptionVerifier {
  private val nonLatinCharacterVerifier = NonLatinCharacterVerifier()

  fun verify(plugin: PluginBean, descriptorPath: String, problemRegistrar: ProblemRegistrar) {
    val htmlDescription = plugin.description

    if (htmlDescription.isNullOrEmpty()) {
      problemRegistrar.registerProblem(PropertyNotSpecified("description", descriptorPath))
      return
    }
    verifyPropertyLength("description", htmlDescription, MAX_LONG_PROPERTY_LENGTH, descriptorPath, problemRegistrar)

    val html = Jsoup.parseBodyFragment(htmlDescription)
    val textDescription = html.text()

    if (DEFAULT_TEMPLATE_DESCRIPTIONS.any { textDescription.contains(it) }) {
      problemRegistrar.registerProblem(PropertyWithDefaultValue(descriptorPath, PropertyWithDefaultValue.DefaultProperty.DESCRIPTION, textDescription))
      return
    }

    nonLatinCharacterVerifier.verify(textDescription, problemRegistrar)

    val links = html.select("[href],img[src]")
    links.forEach { link ->
      val href = link.attr("abs:href")
      val src = link.attr("abs:src")
      if (href.startsWith("http://")) {
        problemRegistrar.registerProblem(HttpLinkInDescription(href))
      }
      if (src.startsWith("http://")) {
        problemRegistrar.registerProblem(HttpLinkInDescription(src))
      }
    }
  }

  class NonLatinCharacterVerifier {
    // \u2013 - `–` (short dash) ans \u2014 - `—` (long dash)
    @Suppress("RegExpSimplifiable")
    private val latinSymbolsRegex = Regex("[\\w\\s\\p{Punct}\\u2013\\u2014]{$MIN_DESCRIPTION_LENGTH,}")

    fun verify(textDescription: String, problemRegistrar: ProblemRegistrar) {
      val latinDescriptionPart = latinSymbolsRegex.find(textDescription)?.value
      if (latinDescriptionPart == null) {
        problemRegistrar.registerProblem(ShortOrNonLatinDescription())
      }
    }
  }
}

