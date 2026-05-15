package com.jetbrains.plugin.structure.base.problems

import org.jsoup.Jsoup

val ALLOWED_NAME_SYMBOLS = Regex("[a-zA-Z0-9 .,+_\\-/:()#'&\\[\\]|]+")
private const val MAX_DESCRIPTION_LENGTH = 65535
private val DEFAULT_TEMPLATE_DESCRIPTIONS = setOf(
  "Enter short description for your plugin here", "most HTML tags may be used", "example.com/my-framework"
)
const val MIN_DESCRIPTION_LENGTH = 40

// \u2013 - `–` (short dash) ans \u2014 - `—` (long dash)
@Suppress("RegExpSimplifiable")
private val STARTS_WITH_LATIN_SYMBOLS_REGEX = Regex("^[\\w\\s\\p{Punct}\\u2013\\u2014]{$MIN_DESCRIPTION_LENGTH,}")

fun validatePropertyLength(
  descriptor: String?,
  propertyName: String,
  propertyValue: String,
  maxLength: Int,
  problems: MutableList<PluginProblem>
) {
  if (propertyValue.length > maxLength) {
    problems.add(TooLongPropertyValue(descriptor, propertyName, propertyValue.length, maxLength))
  }
}

fun validatePluginNameIsCorrect(descriptor: String, name: String, problems: MutableList<PluginProblem>) {
  validatePluginNameIsCorrect(descriptor, name)?.let {
    problems.add(it)
  }
}

fun validatePluginNameIsCorrect(descriptor: String, name: String): PluginProblem? =
  if (!name.matches(ALLOWED_NAME_SYMBOLS)) {
    InvalidPluginName(descriptor, name)
  } else {
    null
  }

fun validateDescriptionIsCorrect(propertyName: String, htmlDescription: String?, descriptorPath: String?): List<PluginProblem> {
  val problems = mutableListOf<PluginProblem>()
  if (htmlDescription.isNullOrEmpty()) {
    problems.add(PropertyNotSpecified(propertyName, descriptorPath))
    return problems
  }
  validatePropertyLength(descriptorPath, propertyName, htmlDescription, MAX_DESCRIPTION_LENGTH, problems)

  val html = Jsoup.parseBodyFragment(htmlDescription)
  val textDescription = html.text()

  if (DEFAULT_TEMPLATE_DESCRIPTIONS.any { textDescription.contains(it) }) {
    problems.add(
      PropertyWithDefaultValue(
        descriptorPath,
        PropertyWithDefaultValue.DefaultProperty.DESCRIPTION,
        textDescription
      )
    )
    return problems
  }

  val latinDescriptionPart = STARTS_WITH_LATIN_SYMBOLS_REGEX.find(textDescription)?.value
  if (latinDescriptionPart == null) {
    problems.add(DescriptionNotStartingWithLatinCharacters())
  }
  val links = html.select("[href],img[src]")
  links.forEach { link ->
    val href = link.attr("abs:href")
    val src = link.attr("abs:src")
    if (href.startsWith("http://")) {
      problems.add(HttpLinkInDescription(href))
    }
    if (src.startsWith("http://")) {
      problems.add(HttpLinkInDescription(src))
    }
  }
  return problems
}