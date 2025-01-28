package com.jetbrains.plugin.structure.teamcity.recipe

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode

internal class RecipeContainerDeserializer : JsonDeserializer<RecipeContainerDescriptor?>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): RecipeContainerDescriptor? {
        val node: JsonNode = parser.codec.readTree(parser)
        return when {
            node.isTextual -> RecipeContainerDescriptor(image = node.asText())
            node.isObject -> context.readTreeAsValue(node, RecipeContainerDescriptor::class.java)
            else -> null
        }
    }
}
