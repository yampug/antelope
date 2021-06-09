import io.css.antelope.Configurator
import io.css.antelope.Minifier
import io.css.antelope.core.Constants.Companion.ASCII_LOGO
import io.css.antelope.core.Core
import org.apache.commons.io.FileUtils
import java.io.File

fun main() {
    println(ASCII_LOGO)
    val config = Configurator.parse(FileUtils.readFileToString(File("./config.json"), Charsets.UTF_8))
    println("Config:$config")

    println("Generating css...")
    val output = Core().generate(config)
    FileUtils.writeStringToFile(File("build/antelope.css"), output, Charsets.UTF_8)
    println("Css generated of len ${output.length} chars")

    Minifier().minify()
}