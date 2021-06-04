package io.css.antelope.core

enum class Size(val prefix: String, val pixel: Int) {
    SMALL("s", 640),
    MEDIUM("m", 768),
    LARGE("l", 1024),
    EXTRA_LARGE("x", 1280)
}