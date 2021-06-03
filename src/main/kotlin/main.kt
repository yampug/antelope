import io.css.antelope.Constants.Companion.ASCII_LOGO
import io.css.antelope.Core
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

fun main(args: Array<String>) {
    println(ASCII_LOGO)
    println("Generating css...")
    val output = Core().generate()
    println("Css generated of len ${output.length} chars")

    // Create Content
    val content = StringBuilder().apply {
        append(URLEncoder.encode("input", "UTF-8"))
        append("=")
        append(URLEncoder.encode(output, "UTF-8"))
    }.toString()

    // Create Request
    println("Requesting minification...")
    val request = (URL("https://cssminifier.com/raw").openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        doOutput = true
        setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        setRequestProperty("charset", "utf-8")
        setRequestProperty("Content-Length", content.length.toString())
        OutputStreamWriter(outputStream).apply {
            write(content)
            flush()
        }
    }

    // Parse Response
    if(request.responseCode == 200) {
        val minifiedCss = InputStreamReader(request.inputStream).readText()
        println("Minified of len ${minifiedCss.length} characters")
        FileUtils.writeStringToFile(File("./build/antelope.css"), minifiedCss, Charsets.UTF_8)
    }

    // Handle Error
    else println("Error: ${request.responseCode} ${request.responseMessage}")
}