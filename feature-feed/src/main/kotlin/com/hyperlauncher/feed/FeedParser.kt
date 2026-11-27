package com.hyperlauncher.feed

import java.io.StringReader
import java.net.URI
import javax.xml.parsers.SAXParserFactory
import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.helpers.DefaultHandler

/** RSS 2.0 and Atom, plain-text presentation only; no external entities or HTML rendering. */
data class Article(val url: String,val title: String,val published: String)
object FeedParser {
    const val MAX_BYTES=2*1024*1024
    fun safeUrl(url: String): Boolean = runCatching {
        val parsed=URI(url);parsed.scheme=="https" && !parsed.host.isNullOrBlank() && parsed.userInfo==null
    }.getOrDefault(false)
    fun parse(xml: String,baseUrl: String): List<Article> {
        require(xml.toByteArray().size<=MAX_BYTES) {"Feed exceeds 2 MiB"}
        require(!xml.contains("<!DOCTYPE",true) && !xml.contains("<!ENTITY",true)) {"DTD/entity declarations are not allowed"}
        require(safeUrl(baseUrl))
        val output=mutableListOf<Article>()
        val handler=object : DefaultHandler() {
            var entryDepth=0;var depth=0;var field="";var title="";var link="";var published=""
            val text=StringBuilder()
            override fun startElement(uri: String?,local: String?,qualified: String?,attributes: Attributes) {
                depth++
                val tag=(local?.takeIf {it.isNotBlank()} ?: qualified.orEmpty()).substringAfter(':')
                if(tag=="item" || tag=="entry") {entryDepth=depth;title="";link="";published=""}
                if(entryDepth>0 && depth==entryDepth+1) {
                    field=tag;text.setLength(0)
                    if(tag=="link" && attributes.getValue("rel") in listOf(null,"alternate"))
                        attributes.getValue("href")?.let {link=it}
                }
            }
            override fun characters(ch: CharArray,start: Int,length: Int) { if(entryDepth>0 && text.length<32000) text.append(ch,start,length.coerceAtMost(32000-text.length)) }
            override fun endElement(uri: String?,local: String?,qualified: String?) {
                if(entryDepth>0 && depth==entryDepth+1) {
                    val value=text.toString().trim()
                    when(field) {"title" -> title=value;"link" -> if(link.isBlank()) link=value;"pubDate","published","updated" -> if(published.isBlank()) published=value}
                    field="";text.setLength(0)
                }
                if(entryDepth>0 && depth==entryDepth) {
                    val absolute=runCatching {URI(baseUrl).resolve(link).toString()}.getOrDefault("")
                    if(link.isNotBlank() && safeUrl(absolute) && output.size<500) output.add(Article(absolute,title.ifBlank {absolute}.take(500),published.take(120)))
                    entryDepth=0
                }
                depth--
            }
        }
        val reader=SAXParserFactory.newInstance().apply {isNamespaceAware=true}.newSAXParser().xmlReader
        runCatching {reader.setFeature("http://xml.org/sax/features/external-general-entities",false)}
        runCatching {reader.setFeature("http://xml.org/sax/features/external-parameter-entities",false)}
        reader.entityResolver=org.xml.sax.EntityResolver {_,_->InputSource(StringReader(""))}
        reader.contentHandler=handler
        reader.parse(InputSource(StringReader(xml)))
        return output.distinctBy {it.url}
    }
}
