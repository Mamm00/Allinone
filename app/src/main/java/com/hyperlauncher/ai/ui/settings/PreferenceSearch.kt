package com.hyperlauncher.ai.ui.settings
import java.text.Normalizer
import java.util.Locale
fun matchesPreference(label: String,query: String): Boolean {
    fun normalize(value: String)=Normalizer.normalize(value,Normalizer.Form.NFKD)
        .replace(Regex("\\p{M}+"),"").uppercase(Locale.ROOT).lowercase(Locale.ROOT)
    val target=normalize(label)
    return normalize(query).trim().split(Regex("\\s+")).all {it.isBlank() || target.contains(it)}
}
