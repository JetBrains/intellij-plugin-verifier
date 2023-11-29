/*
 * Copyright 2000-2023 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.misc

import java.io.IOException
import java.io.StringWriter
import java.io.Writer
import java.lang.Character.*
import java.util.*

/**
 * HTML 4.0 String escaper.
 *
 * Code from Apache `commons-text`, adjusted for Kotlin and simplified.
 */
class HtmlEscaper {
    /** The mapping to be used in translation.  */
    private val lookupMap = mutableMapOf<CharSequence, CharSequence>()

    /** The first character of each key in the lookupMap.  */
    private val prefixSet = BitSet()

    /** The length of the shortest key in the lookupMap.  */
    private val shortest: Int

    /** The length of the longest key in the lookupMap.  */
    private val longest: Int

    init {
        var currentShortest = Int.MAX_VALUE
        var currentLongest = 0
        for ((key, value) in escapeLookup) {
            this.lookupMap[key.toString()] = value.toString()
            prefixSet.set(key[0].code)
            val sz = key.length
            if (sz < currentShortest) {
                currentShortest = sz
            }
            if (sz > currentLongest) {
                currentLongest = sz
            }
        }
        shortest = currentShortest
        longest = currentLongest
    }

    fun escape(input: CharSequence): String {
        return StringWriter(input.length * 2).run {
            escape(input, this)
            this.toString()
        }
    }


    @Throws(IOException::class)
    private fun escape(input: CharSequence, index: Int, writer: Writer): Int {
        // check if translation exists for the input at position index
        if (!prefixSet[input[index].code]) return 0

        var max = longest
        if (index + longest > input.length) {
            max = input.length - index
        }
        // implement greedy algorithm by trying maximum match first
        for (i in max downTo shortest) {
            val subSeq = input.subSequence(index, index + i)
            val result = lookupMap[subSeq.toString()]
            if (result != null) {
                writer.write(result.toString())
                return Character.codePointCount(subSeq, 0, subSeq.length)
            }
        }
        return 0
    }

    @Throws(IOException::class)
    private fun escape(input: CharSequence, writer: Writer) {
        var pos = 0
        val len = input.length
        while (pos < len) {
            val consumed = escape(input, pos, writer)
            if (consumed == 0) {
                // inlined implementation of Character.toChars(Character.codePointAt(input, pos))
                // avoids allocating temp char arrays and duplicate checks
                val c1 = input[pos]
                writer.write(c1.code)
                pos++
                if (isHighSurrogate(c1) && pos < len) {
                    val c2 = input[pos]
                    if (isLowSurrogate(c2)) {
                        writer.write(c2.code)
                        pos++
                    }
                }
                continue
            }
            // contract with translators is that they have to understand code points
            // and they just took care of a surrogate pair
            repeat(consumed) {
                pos += charCount(codePointAt(input, pos))
            }
        }
    }
}

private val escapeLookup: Map<CharSequence, CharSequence> = mapOf(
  "\u00A0" to "&nbsp;",
  "\u00A1" to "&iexcl;",
  "\u00A2" to "&cent;",
  "\u00A3" to "&pound;",
  "\u00A4" to "&curren;",
  "\u00A5" to "&yen;",
  "\u00A6" to "&brvbar;",
  "\u00A7" to "&sect;",
  "\u00A8" to "&uml;",
  "\u00A9" to "&copy;",
  "\u00AA" to "&ordf;",
  "\u00AB" to "&laquo;",
  "\u00AC" to "&not;",
  "\u00AD" to "&shy;",
  "\u00AE" to "&reg;",
  "\u00AF" to "&macr;",
  "\u00B0" to "&deg;",
  "\u00B1" to "&plusmn;",
  "\u00B2" to "&sup2;",
  "\u00B3" to "&sup3;",
  "\u00B4" to "&acute;",
  "\u00B5" to "&micro;",
  "\u00B6" to "&para;",
  "\u00B7" to "&middot;",
  "\u00B8" to "&cedil;",
  "\u00B9" to "&sup1;",
  "\u00BA" to "&ordm;",
  "\u00BB" to "&raquo;",
  "\u00BC" to "&frac14;",
  "\u00BD" to "&frac12;",
  "\u00BE" to "&frac34;",
  "\u00BF" to "&iquest;",
  "\u00C0" to "&Agrave;",
  "\u00C1" to "&Aacute;",
  "\u00C2" to "&Acirc;",
  "\u00C3" to "&Atilde;",
  "\u00C4" to "&Auml;",
  "\u00C5" to "&Aring;",
  "\u00C6" to "&AElig;",
  "\u00C7" to "&Ccedil;",
  "\u00C8" to "&Egrave;",
  "\u00C9" to "&Eacute;",
  "\u00CA" to "&Ecirc;",
  "\u00CB" to "&Euml;",
  "\u00CC" to "&Igrave;",
  "\u00CD" to "&Iacute;",
  "\u00CE" to "&Icirc;",
  "\u00CF" to "&Iuml;",
  "\u00D0" to "&ETH;",
  "\u00D1" to "&Ntilde;",
  "\u00D2" to "&Ograve;",
  "\u00D3" to "&Oacute;",
  "\u00D4" to "&Ocirc;",
  "\u00D5" to "&Otilde;",
  "\u00D6" to "&Ouml;",
  "\u00D7" to "&times;",
  "\u00D8" to "&Oslash;",
  "\u00D9" to "&Ugrave;",
  "\u00DA" to "&Uacute;",
  "\u00DB" to "&Ucirc;",
  "\u00DC" to "&Uuml;",
  "\u00DD" to "&Yacute;",
  "\u00DE" to "&THORN;",
  "\u00DF" to "&szlig;",
  "\u00E0" to "&agrave;",
  "\u00E1" to "&aacute;",
  "\u00E2" to "&acirc;",
  "\u00E3" to "&atilde;",
  "\u00E4" to "&auml;",
  "\u00E5" to "&aring;",
  "\u00E6" to "&aelig;",
  "\u00E7" to "&ccedil;",
  "\u00E8" to "&egrave;",
  "\u00E9" to "&eacute;",
  "\u00EA" to "&ecirc;",
  "\u00EB" to "&euml;",
  "\u00EC" to "&igrave;",
  "\u00ED" to "&iacute;",
  "\u00EE" to "&icirc;",
  "\u00EF" to "&iuml;",
  "\u00F0" to "&eth;",
  "\u00F1" to "&ntilde;",
  "\u00F2" to "&ograve;",
  "\u00F3" to "&oacute;",
  "\u00F4" to "&ocirc;",
  "\u00F5" to "&otilde;",
  "\u00F6" to "&ouml;",
  "\u00F7" to "&divide;",
  "\u00F8" to "&oslash;",
  "\u00F9" to "&ugrave;",
  "\u00FA" to "&uacute;",
  "\u00FB" to "&ucirc;",
  "\u00FC" to "&uuml;",
  "\u00FD" to "&yacute;",
  "\u00FE" to "&thorn;",
  "\u00FF" to "&yuml;",
  // HTML 4.0 Extended
  "\u0192" to "&fnof;",
  "\u0391" to "&Alpha;",
  "\u0392" to "&Beta;",
  "\u0393" to "&Gamma;",
  "\u0394" to "&Delta;",
  "\u0395" to "&Epsilon;",
  "\u0396" to "&Zeta;",
  "\u0397" to "&Eta;",
  "\u0398" to "&Theta;",
  "\u0399" to "&Iota;",
  "\u039A" to "&Kappa;",
  "\u039B" to "&Lambda;",
  "\u039C" to "&Mu;",
  "\u039D" to "&Nu;",
  "\u039E" to "&Xi;",
  "\u039F" to "&Omicron;",
  "\u03A0" to "&Pi;",
  "\u03A1" to "&Rho;",
  "\u03A3" to "&Sigma;",
  "\u03A4" to "&Tau;",
  "\u03A5" to "&Upsilon;",
  "\u03A6" to "&Phi;",
  "\u03A7" to "&Chi;",
  "\u03A8" to "&Psi;",
  "\u03A9" to "&Omega;",
  "\u03B1" to "&alpha;",
  "\u03B2" to "&beta;",
  "\u03B3" to "&gamma;",
  "\u03B4" to "&delta;",
  "\u03B5" to "&epsilon;",
  "\u03B6" to "&zeta;",
  "\u03B7" to "&eta;",
  "\u03B8" to "&theta;",
  "\u03B9" to "&iota;",
  "\u03BA" to "&kappa;",
  "\u03BB" to "&lambda;",
  "\u03BC" to "&mu;",
  "\u03BD" to "&nu;",
  "\u03BE" to "&xi;",
  "\u03BF" to "&omicron;",
  "\u03C0" to "&pi;",
  "\u03C1" to "&rho;",
  "\u03C2" to "&sigmaf;",
  "\u03C3" to "&sigma;",
  "\u03C4" to "&tau;",
  "\u03C5" to "&upsilon;",
  "\u03C6" to "&phi;",
  "\u03C7" to "&chi;",
  "\u03C8" to "&psi;",
  "\u03C9" to "&omega;",
  "\u03D1" to "&thetasym;",
  "\u03D2" to "&upsih;",
  "\u03D6" to "&piv;",
  "\u2022" to "&bull;",
  "\u2026" to "&hellip;",
  "\u2032" to "&prime;",
  "\u2033" to "&Prime;",
  "\u203E" to "&oline;",
  "\u2044" to "&frasl;",
  "\u2118" to "&weierp;",
  "\u2111" to "&image;",
  "\u211C" to "&real;",
  "\u2122" to "&trade;",
  "\u2135" to "&alefsym;",
  "\u2190" to "&larr;",
  "\u2191" to "&uarr;",
  "\u2192" to "&rarr;",
  "\u2193" to "&darr;",
  "\u2194" to "&harr;",
  "\u21B5" to "&crarr;",
  "\u21D0" to "&lArr;",
  "\u21D1" to "&uArr;",
  "\u21D2" to "&rArr;",
  "\u21D3" to "&dArr;",
  "\u21D4" to "&hArr;",
  "\u2200" to "&forall;",
  "\u2202" to "&part;",
  "\u2203" to "&exist;",
  "\u2205" to "&empty;",
  "\u2207" to "&nabla;",
  "\u2208" to "&isin;",
  "\u2209" to "&notin;",
  "\u220B" to "&ni;",
  "\u220F" to "&prod;",
  "\u2211" to "&sum;",
  "\u2212" to "&minus;",
  "\u2217" to "&lowast;",
  "\u221A" to "&radic;",
  "\u221D" to "&prop;",
  "\u221E" to "&infin;",
  "\u2220" to "&ang;",
  "\u2227" to "&and;",
  "\u2228" to "&or;",
  "\u2229" to "&cap;",
  "\u222A" to "&cup;",
  "\u222B" to "&int;",
  "\u2234" to "&there4;",
  "\u223C" to "&sim;",
  "\u2245" to "&cong;",
  "\u2248" to "&asymp;",
  "\u2260" to "&ne;",
  "\u2261" to "&equiv;",
  "\u2264" to "&le;",
  "\u2265" to "&ge;",
  "\u2282" to "&sub;",
  "\u2283" to "&sup;",
  "\u2284" to "&nsub;",
  "\u2286" to "&sube;",
  "\u2287" to "&supe;",
  "\u2295" to "&oplus;",
  "\u2297" to "&otimes;",
  "\u22A5" to "&perp;",
  "\u22C5" to "&sdot;",
  "\u2308" to "&lceil;",
  "\u2309" to "&rceil;",
  "\u230A" to "&lfloor;",
  "\u230B" to "&rfloor;",
  "\u2329" to "&lang;",
  "\u232A" to "&rang;",
  "\u25CA" to "&loz;",
  "\u2660" to "&spades;",
  "\u2663" to "&clubs;",
  "\u2665" to "&hearts;",
  "\u2666" to "&diams;",
  "\u0152" to "&OElig;",
  "\u0153" to "&oelig;",
  "\u0160" to "&Scaron;",
  "\u0161" to "&scaron;",
  "\u0178" to "&Yuml;",
  "\u02C6" to "&circ;",
  "\u02DC" to "&tilde;",
  "\u2002" to "&ensp;",
  "\u2003" to "&emsp;",
  "\u2009" to "&thinsp;",
  "\u200C" to "&zwnj;",
  "\u200D" to "&zwj;",
  "\u200E" to "&lrm;",
  "\u200F" to "&rlm;",
  "\u2013" to "&ndash;",
  "\u2014" to "&mdash;",
  "\u2018" to "&lsquo;",
  "\u2019" to "&rsquo;",
  "\u201A" to "&sbquo;",
  "\u201C" to "&ldquo;",
  "\u201D" to "&rdquo;",
  "\u201E" to "&bdquo;",
  "\u2020" to "&dagger;",
  "\u2021" to "&Dagger;",
  "\u2030" to "&permil;",
  "\u2039" to "&lsaquo;",
  "\u203A" to "&rsaquo;",
  "\u20AC" to "&euro;",
  // basic escapes
  "\"" to "&quot;",
  "&" to "&amp;",
  "<" to "&lt;",
  ">" to "&gt;"
)

fun String.escapeHtml4(): String {
    return HtmlEscaper().escape(this)
}