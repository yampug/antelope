/*
 * YUI Compressor
 * http://developer.yahoo.com/yui/compressor/
 * Author: Julien Lecomte -  http://www.julienlecomte.net/
 * Author: Isaac Schlueter - http://foohack.com/
 * Author: Stoyan Stefanov - http://phpied.com/
 * Contributor: Dan Beam - http://danbeam.org/
 * Copyright (c) 2013 Yahoo! Inc.  All rights reserved.
 * The copyrights embodied in the content of this file are licensed
 * by Yahoo! Inc. under the BSD (revised) open source license.
 */
package com.yahoo.platform.yui.compressor

import java.lang.StringBuffer
import java.util.ArrayList
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Modified version of the excellent CssCompressor from YUI Compressor in Kotlin
 */
class CssCompressor(css: String?) {
    private val strBuffer = StringBuffer()

    /**
     * @param css - full css string
     * @param preservedToken - token to preserve
     * @param tokenRegex - regex to find token
     * @param removeWhiteSpace - remove any white space in the token
     * @param preservedTokens - array of token values
     * @return
     */
    protected fun preserveToken(
        css: String, preservedToken: String,
        tokenRegex: String?, removeWhiteSpace: Boolean, preservedTokens: ArrayList<String>
    ): String {
        val maxIndex = css.length - 1
        var appendIndex = 0
        val sb = StringBuffer()
        val p = Pattern.compile(tokenRegex)
        val m = p.matcher(css)
        while (m.find()) {
            val startIndex = m.start() + (preservedToken.length + 1)
            var terminator = m.group(1)

            // skip this, if CSS was already copied to "sb" upto this position
            if (m.start() < appendIndex) {
                continue
            }
            if (terminator.length == 0) {
                terminator = ")"
            }
            var foundTerminator = false
            var endIndex = m.end() - 1
            while (foundTerminator == false && endIndex + 1 <= maxIndex) {
                endIndex = css.indexOf(terminator, endIndex + 1)
                if (endIndex <= 0) {
                    break
                } else if (endIndex > 0 && css[endIndex - 1] != '\\') {
                    foundTerminator = true
                    if (")" != terminator) {
                        endIndex = css.indexOf(")", endIndex)
                    }
                }
            }

            // Enough searching, start moving stuff over to the buffer
            sb.append(css.substring(appendIndex, m.start()))
            if (foundTerminator) {
                var token = css.substring(startIndex, endIndex)
                if (removeWhiteSpace) token = token.replace("\\s+".toRegex(), "")
                preservedTokens.add(token)
                val preserver = preservedToken + "(___YUICSSMIN_PRESERVED_TOKEN_" + (preservedTokens.size - 1) + "___)"
                sb.append(preserver)
                appendIndex = endIndex + 1
            } else {
                // No end terminator found, re-add the whole match. Should we throw/warn here?
                sb.append(css.substring(m.start(), m.end()))
                appendIndex = m.end()
            }
        }
        sb.append(css.substring(appendIndex))
        return sb.toString()
    }

    fun compress(): String {
        var p: Pattern
        var m: Matcher
        var css = strBuffer.toString()
        var startIndex = 0
        var endIndex = 0
        var i = 0
        var max = 0
        val preservedTokens: ArrayList<String> = ArrayList<String>(0)
        val comments: ArrayList<String> = ArrayList<String>(0)
        var token: String
        val totallen = css.length
        var placeholder: String
        var sb = StringBuffer(css)

        // collect all comment blocks...
        while (sb.indexOf("/*", startIndex).also { startIndex = it } >= 0) {
            endIndex = sb.indexOf("*/", startIndex + 2)
            if (endIndex < 0) {
                endIndex = totallen
            }
            token = sb.substring(startIndex + 2, endIndex)
            comments.add(token)
            sb.replace(
                startIndex + 2,
                endIndex,
                "___YUICSSMIN_PRESERVE_CANDIDATE_COMMENT_" + (comments.size - 1) + "___"
            )
            startIndex += 2
        }
        css = sb.toString()
        css = preserveToken(css, "url", "(?i)url\\(\\s*([\"']?)data\\:", true, preservedTokens)
        css = preserveToken(css, "calc", "(?i)calc\\(\\s*([\"']?)", false, preservedTokens)
        css = preserveToken(
            css,
            "progid:DXImageTransform.Microsoft.Matrix",
            "(?i)progid:DXImageTransform.Microsoft.Matrix\\s*([\"']?)",
            false,
            preservedTokens
        )


        // preserve strings so their content doesn't get accidentally minified
        sb = StringBuffer()
        p = Pattern.compile("(\"([^\\\\\"]|\\\\.|\\\\)*\")|(\'([^\\\\\']|\\\\.|\\\\)*\')")
        m = p.matcher(css)
        while (m.find()) {
            token = m.group()
            val quote = token[0]
            token = token.substring(1, token.length - 1)

            // maybe the string contains a comment-like substring?
            // one, maybe more? put'em back then
            if (token.indexOf("___YUICSSMIN_PRESERVE_CANDIDATE_COMMENT_") >= 0) {
                i = 0
                max = comments.size
                while (i < max) {
                    token =
                        token.replace("___YUICSSMIN_PRESERVE_CANDIDATE_COMMENT_" + i + "___", comments[i].toString())
                    i += 1
                }
            }

            // minify alpha opacity in filter strings
            token = token.replace("(?i)progid:DXImageTransform.Microsoft.Alpha\\(Opacity=".toRegex(), "alpha(opacity=")
            preservedTokens.add(token)
            val preserver =
                quote.toString() + "___YUICSSMIN_PRESERVED_TOKEN_" + (preservedTokens.size - 1) + "___" + quote
            m.appendReplacement(sb, preserver)
        }
        m.appendTail(sb)
        css = sb.toString()


        // strings are safe, now wrestle the comments
        i = 0
        max = comments.size
        while (i < max) {
            token = comments[i].toString()
            placeholder = "___YUICSSMIN_PRESERVE_CANDIDATE_COMMENT_" + i + "___"

            // ! in the first position of the comment means preserve
            // so push to the preserved tokens while stripping the !
            if (token.startsWith("!")) {
                preservedTokens.add(token)
                css = css.replace(placeholder, "___YUICSSMIN_PRESERVED_TOKEN_" + (preservedTokens.size - 1) + "___")
                i += 1
                continue
            }

            // \ in the last position looks like hack for Mac/IE5
            // shorten that to /*\*/ and the next one to /**/
            if (token.endsWith("\\")) {
                preservedTokens.add("\\")
                css = css.replace(placeholder, "___YUICSSMIN_PRESERVED_TOKEN_" + (preservedTokens.size - 1) + "___")
                i = i + 1 // attn: advancing the loop
                preservedTokens.add("")
                css = css.replace(
                    "___YUICSSMIN_PRESERVE_CANDIDATE_COMMENT_" + i + "___",
                    "___YUICSSMIN_PRESERVED_TOKEN_" + (preservedTokens.size - 1) + "___"
                )
                i += 1
                continue
            }

            // keep empty comments after child selectors (IE7 hack)
            // e.g. html >/**/ body
            if (token.length == 0) {
                startIndex = css.indexOf(placeholder)
                if (startIndex > 2) {
                    if (css[startIndex - 3] == '>') {
                        preservedTokens.add("")
                        css = css.replace(
                            placeholder,
                            "___YUICSSMIN_PRESERVED_TOKEN_" + (preservedTokens.size - 1) + "___"
                        )
                    }
                }
            }

            // in all other cases kill the comment
            css = css.replace("/*$placeholder*/", "")
            i += 1
        }

        // preserve \9 IE hack
        val backslash9 = "\\9"
        while (css.indexOf(backslash9) > -1) {
            preservedTokens.add(backslash9)
            css = css.replace(backslash9, "___YUICSSMIN_PRESERVED_TOKEN_" + (preservedTokens.size - 1) + "___")
        }

        // Normalize all whitespace strings to single spaces. Easier to work with that way.
        css = css.replace("\\s+".toRegex(), " ")

        // Remove the spaces before the things that should not have spaces before them.
        // But, be careful not to turn "p :link {...}" into "p:link{...}"
        // Swap out any pseudo-class colons with the token, and then swap back.
        sb = StringBuffer()
        p = Pattern.compile("(^|\\})((^|([^\\{:])+):)+([^\\{]*\\{)")
        m = p.matcher(css)
        while (m.find()) {
            var s = m.group()
            s = s.replace(":".toRegex(), "___YUICSSMIN_PSEUDOCLASSCOLON___")
            s = s.replace("\\\\".toRegex(), "\\\\\\\\").replace("\\$".toRegex(), "\\\\\\$")
            m.appendReplacement(sb, s)
        }
        m.appendTail(sb)
        css = sb.toString()
        // Remove spaces before the things that should not have spaces before them.
        css = css.replace("\\s+([!{};:>+\\(\\)\\],])".toRegex(), "$1")
        // Restore spaces for !important
        css = css.replace("!important".toRegex(), " !important")
        // bring back the colon
        css = css.replace("___YUICSSMIN_PSEUDOCLASSCOLON___".toRegex(), ":")

        // retain space for special IE6 cases
        sb = StringBuffer()
        p = Pattern.compile("(?i):first\\-(line|letter)(\\{|,)")
        m = p.matcher(css)
        while (m.find()) {
            m.appendReplacement(sb, ":first-" + m.group(1).toLowerCase() + " " + m.group(2))
        }
        m.appendTail(sb)
        css = sb.toString()

        // no space after the end of a preserved comment
        css = css.replace("\\*/ ".toRegex(), "*/")

        // If there are multiple @charset directives, push them to the top of the file.
        sb = StringBuffer()
        p = Pattern.compile("(?i)^(.*)(@charset)( \"[^\"]*\";)")
        m = p.matcher(css)
        while (m.find()) {
            val s = m.group(1).replace("\\\\".toRegex(), "\\\\\\\\").replace("\\$".toRegex(), "\\\\\\$")
            m.appendReplacement(sb, m.group(2).toLowerCase() + m.group(3) + s)
        }
        m.appendTail(sb)
        css = sb.toString()

        // When all @charset are at the top, remove the second and after (as they are completely ignored).
        sb = StringBuffer()
        p = Pattern.compile("(?i)^((\\s*)(@charset)( [^;]+;\\s*))+")
        m = p.matcher(css)
        while (m.find()) {
            m.appendReplacement(sb, m.group(2) + m.group(3).toLowerCase() + m.group(4))
        }
        m.appendTail(sb)
        css = sb.toString()

        // lowercase some popular @directives (@charset is done right above)
        sb = StringBuffer()
        p =
            Pattern.compile("(?i)@(font-face|import|(?:-(?:atsc|khtml|moz|ms|o|wap|webkit)-)?keyframe|media|page|namespace)")
        m = p.matcher(css)
        while (m.find()) {
            m.appendReplacement(sb, '@'.toString() + m.group(1).toLowerCase())
        }
        m.appendTail(sb)
        css = sb.toString()

        // lowercase some more common pseudo-elements
        sb = StringBuffer()
        p =
            Pattern.compile("(?i):(active|after|before|checked|disabled|empty|enabled|first-(?:child|of-type)|focus|hover|last-(?:child|of-type)|link|only-(?:child|of-type)|root|:selection|target|visited)")
        m = p.matcher(css)
        while (m.find()) {
            m.appendReplacement(sb, ':'.toString() + m.group(1).toLowerCase())
        }
        m.appendTail(sb)
        css = sb.toString()

        // lowercase some more common functions
        sb = StringBuffer()
        p =
            Pattern.compile("(?i):(lang|not|nth-child|nth-last-child|nth-last-of-type|nth-of-type|(?:-(?:moz|webkit)-)?any)\\(")
        m = p.matcher(css)
        while (m.find()) {
            m.appendReplacement(sb, ':'.toString() + m.group(1).toLowerCase() + '(')
        }
        m.appendTail(sb)
        css = sb.toString()

        // lower case some common function that can be values
        // NOTE: rgb() isn't useful as we replace with #hex later, as well as and() is already done for us right after this
        sb = StringBuffer()
        p =
            Pattern.compile("(?i)([:,\\( ]\\s*)(attr|color-stop|from|rgba|to|url|(?:-(?:atsc|khtml|moz|ms|o|wap|webkit)-)?(?:calc|max|min|(?:repeating-)?(?:linear|radial)-gradient)|-webkit-gradient)")
        m = p.matcher(css)
        while (m.find()) {
            m.appendReplacement(sb, m.group(1) + m.group(2).toLowerCase())
        }
        m.appendTail(sb)
        css = sb.toString()

        // Put the space back in some cases, to support stuff like
        // @media screen and (-webkit-min-device-pixel-ratio:0){
        css = css.replace("(?i)\\band\\(".toRegex(), "and (")

        // Remove the spaces after the things that should not have spaces after them.
        css = css.replace("([!{}:;>+\\(\\[,])\\s+".toRegex(), "$1")

        // remove unnecessary semicolons
        css = css.replace(";+}".toRegex(), "}")

        // Replace 0(px,em) with 0. (don't replace seconds are they are needed for transitions to be valid)
        var oldCssA: String
        p = Pattern.compile("(?i)(^|: ?)((?:[0-9a-z-.]+ )*?)?(?:0?\\.)?0(?:px|em|in|cm|mm|pc|pt|ex|deg|g?rad|k?hz)")
        do {
            oldCssA = css
            m = p.matcher(css)
            css = m.replaceAll("$1$20")
        } while (css != oldCssA)

        // We do the same with % but don't replace the 0% in keyframes
        var oldCssB: String
        p = Pattern.compile("(?i)(: ?)((?:[0-9a-z-.]+ )*?)?(?:0?\\.)?0(?:%)")
        do {
            oldCssB = css
            m = p.matcher(css)
            css = m.replaceAll("$1$20")
        } while (css != oldCssB)

        //Replace the keyframe 100% step with 'to' which is shorter
        p = Pattern.compile("(?i)(^|,|\\{) ?(?:100% ?\\{)")
        do {
            oldCssB = css
            m = p.matcher(css)
            css = m.replaceAll("$1to{")
        } while (css != oldCssB)

        // Replace 0(px,em,%) with 0 inside groups (e.g. -MOZ-RADIAL-GRADIENT(CENTER 45DEG, CIRCLE CLOSEST-SIDE, ORANGE 0%, RED 100%))
        p =
            Pattern.compile("(?i)\\( ?((?:[0-9a-z-.]+[ ,])*)?(?:0?\\.)?0(?:px|em|%|in|cm|mm|pc|pt|ex|deg|g?rad|m?s|k?hz)")
        do {
            oldCssB = css
            m = p.matcher(css)
            css = m.replaceAll("($10")
        } while (css != oldCssB)

        // Replace x.0(px,em,%) with x(px,em,%).
        css = css.replace("([0-9])\\.0(px|em|%|in|cm|mm|pc|pt|ex|deg|m?s|g?rad|k?hz| |;)".toRegex(), "$1$2")

        // Replace 0 0 0 0; with 0.
        css = css.replace(":0 0 0 0(;|})".toRegex(), ":0$1")
        css = css.replace(":0 0 0(;|})".toRegex(), ":0$1")
        css = css.replace("(?<!flex):0 0(;|})".toRegex(), ":0$1")


        // Replace background-position:0; with background-position:0 0;
        // same for transform-origin
        sb = StringBuffer()
        p =
            Pattern.compile("(?i)(background-position|webkit-mask-position|transform-origin|webkit-transform-origin|moz-transform-origin|o-transform-origin|ms-transform-origin):0(;|})")
        m = p.matcher(css)
        while (m.find()) {
            m.appendReplacement(sb, m.group(1).toLowerCase() + ":0 0" + m.group(2))
        }
        m.appendTail(sb)
        css = sb.toString()

        // Replace 0.6 to .6, but only when preceded by : or a white-space
        css = css.replace("(:|\\s)0+\\.(\\d+)".toRegex(), "$1.$2")

        // Shorten colors from rgb(51,102,153) to #336699
        // This makes it more likely that it'll get further compressed in the next step.
        p = Pattern.compile("rgb\\s*\\(\\s*([0-9,\\s]+)\\s*\\)")
        m = p.matcher(css)
        sb = StringBuffer()
        while (m.find()) {
            val rgbcolors = m.group(1).split(",".toRegex()).toTypedArray()
            val hexcolor = StringBuffer("#")
            i = 0
            while (i < rgbcolors.size) {
                var `val` = rgbcolors[i].toInt()
                if (`val` < 16) {
                    hexcolor.append("0")
                }

                // If someone passes an RGB value that's too big to express in two characters, round down.
                // Probably should throw out a warning here, but generating valid CSS is a bigger concern.
                if (`val` > 255) {
                    `val` = 255
                }
                hexcolor.append(Integer.toHexString(`val`))
                i++
            }
            m.appendReplacement(sb, hexcolor.toString())
        }
        m.appendTail(sb)
        css = sb.toString()

        // Shorten colors from #AABBCC to #ABC. Note that we want to make sure
        // the color is not preceded by either ", " or =. Indeed, the property
        //     filter: chroma(color="#FFFFFF");
        // would become
        //     filter: chroma(color="#FFF");
        // which makes the filter break in IE.
        // We also want to make sure we're only compressing #AABBCC patterns inside { }, not id selectors ( #FAABAC {} )
        // We also want to avoid compressing invalid values (e.g. #AABBCCD to #ABCD)
        p =
            Pattern.compile("(\\=\\s*?[\"']?)?" + "#([0-9a-fA-F])([0-9a-fA-F])([0-9a-fA-F])([0-9a-fA-F])([0-9a-fA-F])([0-9a-fA-F])" + "(:?\\}|[^0-9a-fA-F{][^{]*?\\})")
        m = p.matcher(css)
        sb = StringBuffer()
        var index = 0
        while (m.find(index)) {
            sb.append(css.substring(index, m.start()))
            val isFilter = m.group(1) != null && "" != m.group(1)
            if (isFilter) {
                // Restore, as is. Compression will break filters
                sb.append(m.group(1) + "#" + m.group(2) + m.group(3) + m.group(4) + m.group(5) + m.group(6) + m.group(7))
            } else {
                if (m.group(2).equals(m.group(3), ignoreCase = true) &&
                    m.group(4).equals(m.group(5), ignoreCase = true) &&
                    m.group(6).equals(m.group(7), ignoreCase = true)
                ) {

                    // #AABBCC pattern
                    sb.append("#" + (m.group(3) + m.group(5) + m.group(7)).toLowerCase())
                } else {

                    // Non-compressible color, restore, but lower case.
                    sb.append("#" + (m.group(2) + m.group(3) + m.group(4) + m.group(5) + m.group(6) + m.group(7)).toLowerCase())
                }
            }
            index = m.end(7)
        }
        sb.append(css.substring(index))
        css = sb.toString()

        // Replace #f00 -> red
        css = css.replace("(:|\\s)(#f00)(;|})".toRegex(), "$1red$3")
        // Replace other short color keywords
        css = css.replace("(:|\\s)(#000080)(;|})".toRegex(), "$1navy$3")
        css = css.replace("(:|\\s)(#808080)(;|})".toRegex(), "$1gray$3")
        css = css.replace("(:|\\s)(#808000)(;|})".toRegex(), "$1olive$3")
        css = css.replace("(:|\\s)(#800080)(;|})".toRegex(), "$1purple$3")
        css = css.replace("(:|\\s)(#c0c0c0)(;|})".toRegex(), "$1silver$3")
        css = css.replace("(:|\\s)(#008080)(;|})".toRegex(), "$1teal$3")
        css = css.replace("(:|\\s)(#ffa500)(;|})".toRegex(), "$1orange$3")
        css = css.replace("(:|\\s)(#800000)(;|})".toRegex(), "$1maroon$3")

        // border: none -> border:0
        sb = StringBuffer()
        p =
            Pattern.compile("(?i)(border|border-top|border-right|border-bottom|border-left|outline|background):none(;|})")
        m = p.matcher(css)
        while (m.find()) {
            m.appendReplacement(sb, m.group(1).toLowerCase() + ":0" + m.group(2))
        }
        m.appendTail(sb)
        css = sb.toString()

        // shorter opacity IE filter
        css = css.replace("(?i)progid:DXImageTransform.Microsoft.Alpha\\(Opacity=".toRegex(), "alpha(opacity=")

        // Find a fraction that is used for Opera's -o-device-pixel-ratio query
        // Add token to add the "\" back in later
        css = css.replace("\\(([\\-A-Za-z]+):([0-9]+)\\/([0-9]+)\\)".toRegex(), "($1:$2___YUI_QUERY_FRACTION___$3)")

        // Remove empty rules.
        css = css.replace("[^\\}\\{/;]+\\{\\}".toRegex(), "")

        // Add "\" back to fix Opera -o-device-pixel-ratio query
        css = css.replace("___YUI_QUERY_FRACTION___".toRegex(), "/")

        // Replace multiple semi-colons in a row by a single one
        // See SF bug #1980989
        css = css.replace(";;+".toRegex(), ";")

        // restore preserved comments and strings
        i = 0
        max = preservedTokens.size
        while (i < max) {
            css = css.replace("___YUICSSMIN_PRESERVED_TOKEN_" + i + "___", preservedTokens[i].toString())
            i++
        }

        // Add spaces back in between operators for css calc function
        // https://developer.mozilla.org/en-US/docs/Web/CSS/calc
        // Added by Eric Arnol-Martin (earnolmartin@gmail.com)
        sb = StringBuffer()
        p = Pattern.compile("calc\\([^\\)]*\\)")
        m = p.matcher(css)
        while (m.find()) {
            var s = m.group()
            s = s.replace("(?<=[-|%|px|em|rem|vw|\\d]\\+)\\+".toRegex(), " + ")
            s = s.replace("(?<=[-|%|px|em|rem|vw|\\d]\\+)\\-".toRegex(), " - ")
            s = s.replace("(?<=[-|%|px|em|rem|vw|\\d]\\+)\\*".toRegex(), " * ")
            s = s.replace("(?<=[-|%|px|em|rem|vw|\\d]\\+)\\/".toRegex(), " / ")
            m.appendReplacement(sb, s)
        }
        m.appendTail(sb)
        css = sb.toString()

        // Trim the final string (for any leading or trailing white spaces)
        css = css.trim { it <= ' ' }

        // Write the output...
        return css
    }

    init {
        strBuffer.append(css)
    }
}