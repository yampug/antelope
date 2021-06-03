package io.css.antelope

data class Rule(
    val name: String,
    val attributes: Map<String, String> = mapOf()
)
