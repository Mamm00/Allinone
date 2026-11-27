package com.hyperlauncher.importer

import javax.xml.parsers.SAXParserFactory
import java.io.StringReader
import org.xml.sax.*
import org.xml.sax.helpers.DefaultHandler

data class IconPackMapping(val icons: Map<String,String>,val calendars: Map<String,String>)
object IconPackParser {
    fun component(raw: String): String {
        val value=raw.trim().removePrefix("ComponentInfo{").removeSuffix("}").removePrefix("ComponentInfo(").removeSuffix(")")
        val parts=value.split('/');if(parts.size!=2) return value
        return parts[0]+"/"+(if(parts[1].startsWith('.')) parts[0]+parts[1] else parts[1])
    }
    fun parse(xml: String): IconPackMapping {
        require(xml.length<=4*1024*1024 && !xml.contains("<!DOCTYPE",true) && !xml.contains("<!ENTITY",true))
        val icons=mutableMapOf<String,String>();val calendars=mutableMapOf<String,String>()
        val reader=SAXParserFactory.newInstance().newSAXParser().xmlReader
        reader.entityResolver=EntityResolver {_,_->InputSource(StringReader(""))}
        reader.contentHandler=object:DefaultHandler(){override fun startElement(uri: String?,local: String?,name: String?,attrs: Attributes) {
            val key=attrs.getValue("component")?.let(::component) ?:return
            if(name=="item") attrs.getValue("drawable")?.let {icons[key]=it}
            if(name=="calendar") attrs.getValue("prefix")?.let {calendars[key]=it}
        }}
        reader.parse(InputSource(StringReader(xml)));return IconPackMapping(icons,calendars)
    }
}
