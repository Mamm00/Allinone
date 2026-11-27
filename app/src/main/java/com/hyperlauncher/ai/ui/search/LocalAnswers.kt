package com.hyperlauncher.ai.ui.search

import java.util.Locale
import kotlin.math.abs

/** Small bounded parser, never JavaScript/eval. No I/O, scripts, function calls or exponentiation. */
object LocalAnswers {
    fun answer(query: String): String? = convert(query) ?: calculate(query)
    private fun format(value: Double): String? = value.takeIf { it.isFinite() }?.let {
        String.format(Locale.ROOT,"%.8f",it).trimEnd('0').trimEnd('.')
    }
    fun calculate(expression: String): String? = runCatching {
        require(expression.length in 1..128 && expression.any { it in "+-*/" })
        class Parser(val text: String) {
            var i=0; var depth=0
            fun skip() { while (i<text.length && text[i].isWhitespace()) i++ }
            fun atom(): Double {
                skip(); require(++depth <= 24)
                val value = when {
                    i<text.length && text[i]=='+' -> { i++; atom() }
                    i<text.length && text[i]=='-' -> { i++; -atom() }
                    i<text.length && text[i]=='(' -> { i++; val v=sum(); skip(); require(i<text.length && text[i++]==')'); v }
                    else -> { val start=i; while(i<text.length && (text[i].isDigit() || text[i]=='.')) i++; require(i>start); text.substring(start,i).toDouble() }
                }; depth--; return value
            }
            fun product(): Double { var a=atom(); while(true) { skip(); if(i>=text.length || text[i] !in "*/") return a; val op=text[i++]; val b=atom(); if(op=='/') require(b!=0.0); a=if(op=='*') a*b else a/b } }
            fun sum(): Double { var a=product(); while(true) { skip(); if(i>=text.length || text[i] !in "+-") return a; val op=text[i++]; val b=product(); a=if(op=='+') a+b else a-b } }
        }
        val p=Parser(expression); val result=p.sum(); p.skip(); require(p.i==expression.length); format(result)
    }.getOrNull()
    fun convert(query: String): String? = runCatching {
        val match=Regex("^\\s*(-?[0-9]+(?:\\.[0-9]+)?)\\s*([a-zA-Z]+)\\s+(?:to|in)\\s+([a-zA-Z]+)\\s*$",RegexOption.IGNORE_CASE).matchEntire(query) ?: return null
        val number=match.groupValues[1].toDouble()
        val from=match.groupValues[2].lowercase(Locale.ROOT); val to=match.groupValues[3].lowercase(Locale.ROOT)
        val length=mapOf("m" to 1.0,"meters" to 1.0,"km" to 1000.0,"cm" to .01,"mm" to .001,"mi" to 1609.344,"miles" to 1609.344,"ft" to .3048,"inches" to .0254)
        val weight=mapOf("kg" to 1.0,"g" to .001,"lb" to .45359237,"lbs" to .45359237,"oz" to .028349523125)
        val table=when { from in length && to in length -> length; from in weight && to in weight -> weight; else -> emptyMap() }
        val result=when { table.isNotEmpty() -> number*table.getValue(from)/table.getValue(to)
            from=="c" && to=="f" -> number*9/5+32; from=="f" && to=="c" -> (number-32)*5/9
            else -> return null }
        "${format(result) ?: return null} $to"
    }.getOrNull()
}

object SearchTemplates {
    fun valid(template: String): Boolean = runCatching {
        if (!template.contains("{query}")) return false
        val uri=java.net.URI(template.replace("{query}","test"))
        uri.scheme=="https" && !uri.host.isNullOrBlank() && uri.userInfo==null
    }.getOrDefault(false)
    fun resolve(template: String, query: String): String {
        require(valid(template)); return template.replace("{query}",java.net.URLEncoder.encode(query,"UTF-8"))
    }
}
