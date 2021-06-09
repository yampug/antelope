package io.css.antelope.core

data class Rule(
    val name: String,
    val attributes: Map<String, String> = mapOf()
)
