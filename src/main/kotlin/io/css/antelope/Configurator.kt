package io.css.antelope

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import io.css.antelope.core.Size

class Configurator {

    companion object {

        private val gson = Gson()

        private data class JsonConfig(
            val sizes: List<String>?,
        )

        fun parse(json: String): Config {
            val sizeMapping = HashMap<String, Size>()
            for (size in Size.values()) {
                sizeMapping[size.prefix] = size
            }

            try {
                val jsonConfig = gson.fromJson(json, JsonConfig::class.java)

                if (jsonConfig.sizes != null) {
                    val configuredSizes = HashSet<Size>()
                    for (strSize in jsonConfig.sizes) {
                        if (sizeMapping.containsKey(strSize)) {
                            configuredSizes.add(sizeMapping[strSize]!!)
                        }
                    }
                    return Config(configuredSizes)
                }
            } catch (e: JsonSyntaxException) {
                println("Failed parsing config, using fallback")
            }
            // default config
            return Config(setOf(Size.SMALL, Size.MEDIUM))
        }
    }
}