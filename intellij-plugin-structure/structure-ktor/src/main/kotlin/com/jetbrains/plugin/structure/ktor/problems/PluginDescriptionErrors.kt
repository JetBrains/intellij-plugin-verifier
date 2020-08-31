package kotlin.com.jetbrains.plugin.structure.ktor.problems

import com.jetbrains.plugin.structure.base.problems.InvalidDescriptorProblem

class DocumentationContainsResource(val section: String) : InvalidDescriptorProblem(null) {

  override val detailedMessage
    get() = "Documentation should never contain a url-based resource. Url found in the \"$section\" section"

  override val level
    get() = Level.ERROR

}