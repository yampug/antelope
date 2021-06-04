import io.css.antelope.Configurator
import io.css.antelope.Minifier
import io.css.antelope.core.Constants.Companion.ASCII_LOGO
import io.css.antelope.core.Core

fun main() {
    println(ASCII_LOGO)
    val config = Configurator.parse("{sizes:[\"s\",\"m\"]")
    println("Config:$config")

    println("Generating css...")
    val output = Core().generate(config)
    println("Css generated of len ${output.length} chars")

    Minifier().minify(output)
}