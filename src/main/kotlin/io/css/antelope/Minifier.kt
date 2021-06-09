package io.css.antelope

import com.yahoo.platform.yui.compressor.CssCompressor
import org.apache.commons.io.FileUtils
import java.io.File

class Minifier {

    fun minify() {
        val compressor = CssCompressor(FileUtils.readFileToString(File("build/antelope.css"), Charsets.UTF_8))
        val minFileContent = compressor.compress()
        val minFile = "build/antelope.min.css"
        FileUtils.writeStringToFile(File(minFile), minFileContent, Charsets.UTF_8)
        println("Minified of len ${minFileContent.length} chars")
    }

    private fun minifyWithGlobalUglify(minFile: String): String {
        // keep around in case we get rid of YUI compressor
        try {
            val rt = Runtime.getRuntime()
            val proc = rt.exec(arrayOf("/bin/sh", "-c", "uglifycss build/antelope.css > $minFile"))

            proc.waitFor()
            return FileUtils.readFileToString(File(minFile), Charsets.UTF_8)
        } catch (e: Exception) {
            println("Couldnt minify css: ${e.message}");
            e.printStackTrace()
        }
        return ""
    }
}