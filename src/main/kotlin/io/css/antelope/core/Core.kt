package io.css.antelope.core

import io.css.antelope.Config

class Core {

    fun generate(config: Config): String {
        val rules = mutableListOf<Rule>()
        rules.addAll(genHeight())
        rules.addAll(genWidth())
        rules.addAll(genTopBottom())
        rules.addAll(genFlexCenter())
        rules.addAll(genBorderRadius())
        rules.addAll(genFontSize())
        rules.addAll(genMisc())
        rules.addAll(genFlexDirections())

        // generate output
        val out = StringBuilder()
        // non prefixed rules
        for (rule in rules) {
            out.append(".${rule.name} { ${serializeAttributes(rule)} }\n")
        }

        // prefixed rules
        for (size in config.sizes) {
            out.append("@media (min-width: ${size.pixel}px) {\n")
            out.append(".${size.prefix} {}\n")
            for (rule in rules) {
                out.append(".${size.prefix}\\+${rule.name} { ${serializeAttributes(rule)}}\n")
            }
            out.append("}\n")
        }

        return out.toString()
    }

    private fun serializeAttributes(rule: Rule): String {
        val strAttr = StringBuilder()
        for (attr in rule.attributes.entries) {
            strAttr.append("${attr.key}: ${attr.value}; ")
        }
        return strAttr.toString()
    }

    private fun genWidth(): List<Rule> {
        return genSize("w", "width")
    }

    private fun genHeight(): List<Rule> {
        return genSize("h", "height");
    }

    private fun genSize(prefix: String, attrName: String): List<Rule> {
        val rules = mutableListOf<Rule>()
        for (i in 20..100 step 5) {
            rules.add(Rule("${prefix}-${i}\\%", mapOf(attrName to "${i}%")))
            if (prefix == "h") {
                rules.add(Rule("${prefix}-${i}\\vh", mapOf(attrName to "${i}vh")))
            }
        }

        rules.addAll(
                listOf(
                        Rule("${prefix}-auto", mapOf(attrName to "auto")),
                        Rule("${prefix}-12", mapOf(attrName to "12px")),
                        Rule("${prefix}-16", mapOf(attrName to "16px")),
                        Rule("${prefix}-24", mapOf(attrName to "24px")),
                        Rule("${prefix}-32", mapOf(attrName to "32px")),
                        Rule("${prefix}-36", mapOf(attrName to "36px")),
                        Rule("${prefix}-42", mapOf(attrName to "42px")),
                        Rule("${prefix}-48", mapOf(attrName to "48px")),
                        Rule("${prefix}-64", mapOf(attrName to "64px")),
                        Rule("${prefix}-80", mapOf(attrName to "80px")),
                        Rule("${prefix}-96", mapOf(attrName to "96px")),
                        Rule("${prefix}-128", mapOf(attrName to "128px")),
                        Rule("${prefix}-160", mapOf(attrName to "160px")),
                        Rule("${prefix}-192", mapOf(attrName to "192px")),
                        Rule("${prefix}-288", mapOf(attrName to "288px")),
                        Rule("${prefix}-320", mapOf(attrName to "320px")),
                        Rule("${prefix}-384", mapOf(attrName to "384px")),
                        Rule("${prefix}-512", mapOf(attrName to "512px")),
                        Rule("${prefix}-768", mapOf(attrName to "768px"))
                )
        )
        return rules
    }

    private fun genTopBottom(): List<Rule> {
        val rules = mutableListOf<Rule>()
        rules.addAll(genDistance("top", "top"))
        rules.addAll(genDistance("bot", "bottom"))
        return rules
    }

    private fun genDistance(prefix: String, attrName: String): List<Rule> {
        return listOf(
                Rule("${prefix}-0", mapOf(attrName to "0px")),
                Rule("${prefix}-16", mapOf(attrName to "16px")),
                Rule("${prefix}-32", mapOf(attrName to "32px")),
                Rule("${prefix}-42", mapOf(attrName to "42px")),
                Rule("${prefix}-48", mapOf(attrName to "48px")),
                Rule("${prefix}-96", mapOf(attrName to "96px"))
        )
    }

    private fun genFlexCenter(): List<Rule> {
        return listOf(
                Rule("flex-center",
                        mapOf(
                                "display" to "flex",
                                "justify-content" to "center",
                                "align-items" to "center",
                                "flex-wrap" to "wrap"
                        )),
                Rule("flex-right",
                        mapOf(
                                "display" to "flex",
                                "justify-content" to "flex-end",
                                "align-items" to "center",
                                "flex-wrap" to "wrap"
                        )),
                Rule("flex-evenly",
                        mapOf(
                                "display" to "flex",
                                "justify-content" to "space-evenly",
                                "align-items" to "center",
                                "flex-wrap" to "wrap"
                        )),
                Rule("flex-between",
                        mapOf(
                                "display" to "flex",
                                "justify-content" to "space-between",
                                "align-items" to "center",
                                "flex-wrap" to "wrap"
                        )),
                Rule("flex-around",
                        mapOf(
                                "display" to "flex",
                                "justify-content" to "space-around",
                                "align-items" to "center",
                                "flex-wrap" to "wrap"
                        )),
                Rule("two-cols",
                        mapOf(
                                "flex-direction" to "row"
                        )),
                Rule("two-cols > div, two-cols > span, two-cols > button",
                        mapOf(
                                "width" to "50%"
                        ))
        )
    }

    private fun genFontSize(): List<Rule> {
        return listOf(
                Rule("fs-8", mapOf("font-size" to "8px")),
                Rule("fs-10", mapOf("font-size" to "10px")),
                Rule("fs-12", mapOf("font-size" to "12px")),
                Rule("fs-13", mapOf("font-size" to "13px")),
                Rule("fs-14", mapOf("font-size" to "14px")),
                Rule("fs-15", mapOf("font-size" to "15px")),
                Rule("fs-18", mapOf("font-size" to "18px")),
                Rule("fs-20", mapOf("font-size" to "20px")),
                Rule("fs-24", mapOf("font-size" to "24px")),
                Rule("fs-28", mapOf("font-size" to "28px")),
                Rule("fs-32", mapOf("font-size" to "32px")),
                Rule("fs-40", mapOf("font-size" to "40px")),
                Rule("fs-48", mapOf("font-size" to "48px")),
                Rule("fs-56", mapOf("font-size" to "56px")),
                Rule("fs-64", mapOf("font-size" to "64px")),
                Rule("fs-72", mapOf("font-size" to "72px"))
        )
    }

    private fun genBorderRadius(): List<Rule> {
        return listOf(
                Rule("brd-bbl", mapOf("border-radius" to "30%")),
                Rule("brd-12", mapOf("border-radius" to "12px"))
        )
    }

    private fun genMisc(): List<Rule> {
        return listOf(
            Rule("flex", mapOf("display" to "flex")),
            Rule("grid", mapOf("display" to "grid")),
            Rule("table-cell", mapOf("display" to "table-cell")),
            Rule("show", mapOf("display" to "unset")),
            Rule("hide", mapOf("display" to "none"))
        )
    }

    private fun genFlexDirections(): List<Rule> {
        return listOf(
            Rule("flex-dir-col", mapOf("flex-direction" to "column")),
            Rule("flex-dir-row", mapOf("flex-direction" to "row")),
            Rule("flex-dir-col-rev", mapOf("flex-direction" to "column-reverse"))
        )
    }
}
