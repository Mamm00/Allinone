package com.hyperlauncher.importer

import java.io.StringReader
import javax.xml.parsers.SAXParserFactory
import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.helpers.DefaultHandler

data class ImportedItem(val id: String,val kind: String,val page: Int,val x: Float,val y: Float,
    val width: Float,val height: Float,val packageName: String,val container: String)
data class ImportedLayout(val items: List<ImportedItem>,val dock: List<String>,val gestures: Map<String,String>)

/** XML-form Nova favorites/workspace exports only. Binary SQLite/proprietary exports fail explicitly. */
object NovaXmlParser {
    fun parse(xml: String): ImportedLayout {
        require(xml.toByteArray().size<=2*1024*1024) {"Import XML exceeds 2 MiB"}
        require(!xml.contains("<!DOCTYPE",true) && !xml.contains("<!ENTITY",true)) {"DTD/entity declarations are not allowed"}
        var columns=5f;var rows=5f;var root="";var count=0
        val items=mutableListOf<ImportedItem>();val dock=mutableListOf<Pair<Int,String>>();val gestures=mutableMapOf<String,String>()
        val handler=object : DefaultHandler() {
            override fun startElement(uri: String?,localName: String?,qName: String?,a: Attributes) {
                val tag=(localName?.takeIf {it.isNotBlank()} ?: qName.orEmpty()).substringAfter(':')
                if(root.isEmpty()) {root=tag;columns=(a.getValue("columns")?.toFloatOrNull() ?: 5f).coerceIn(2f,10f);rows=(a.getValue("rows")?.toFloatOrNull() ?:5f).coerceIn(2f,12f)}
                if(tag=="gesture") {val name=a.getValue("name");val action=a.getValue("action");if(name!=null && action!=null) gestures[name.take(100)]=action.take(2000);return}
                if(tag !in listOf("favorite","item","folder","appwidget","shortcut")) return
                require(++count<=1000) {"Too many imported items"}
                fun num(key: String,default: Float=0f)=(a.getValue(key)?.toFloatOrNull() ?:default).takeIf {it.isFinite()} ?: default
                val intent=a.getValue("intent").orEmpty()
                val component=a.getValue("component") ?: Regex("(?:^|;)component=([^;]+)").find(intent)?.groupValues?.get(1).orEmpty()
                val pkg=(a.getValue("packageName") ?: component.substringBefore('/').removePrefix("ComponentInfo{").removeSuffix("}"))
                    .takeIf {it.matches(Regex("[a-zA-Z0-9_.]+"))}.orEmpty()
                val type=a.getValue("itemType")
                val kind=when {tag=="folder" || type=="2" -> "folder";tag=="appwidget" || type=="4" -> "widget";else -> "app"}
                val container=a.getValue("container") ?: "-100"
                val order=num("screen").toInt()
                if(container=="-101") {if(pkg.isNotBlank()) dock.add(order to pkg);return}
                items.add(ImportedItem(a.getValue("_id") ?: a.getValue("id") ?: "item_$count",kind,order.coerceIn(0,8),
                    (num("cellX")/columns).coerceIn(0f,1f),(num("cellY")/rows).coerceIn(0f,1f),
                    (num("spanX",1f)/columns).coerceIn(.12f,1f),(num("spanY",1f)/rows).coerceIn(.08f,1f),pkg,container))
            }
        }
        val reader=SAXParserFactory.newInstance().apply {isNamespaceAware=true}.newSAXParser().xmlReader
        reader.entityResolver=org.xml.sax.EntityResolver {_,_->InputSource(StringReader(""))}
        runCatching {reader.setFeature("http://xml.org/sax/features/external-general-entities",false)}
        runCatching {reader.setFeature("http://xml.org/sax/features/external-parameter-entities",false)}
        reader.contentHandler=handler;reader.parse(InputSource(StringReader(xml)))
        require(root in listOf("favorites","workspace","nova","launcher","backup")) {"Unsupported Nova XML root"}
        require(items.isNotEmpty() || dock.isNotEmpty()) {"No supported launcher items found; no changes made"}
        return ImportedLayout(items,dock.sortedBy {it.first}.map {it.second}.distinct(),gestures)
    }
}

/** Only documented textual aliases are translated. Unknown/private numeric actions stay unmapped. */
object NovaGestureMapper {
    fun map(gestures: Map<String,String>): Map<String,String> = gestures.mapNotNull {(name,action) ->
        val key=when(name.lowercase().replace('-','_')) {
            "up","swipe_up"->"UP";"down","swipe_down"->"DOWN";"double_tap","doubletap"->"DOUBLE_TAP"
            "pinch_in","pinchin"->"PINCH_IN";"pinch_out","pinchout"->"PINCH_OUT"
            "rotate_left"->"ROTATE_LEFT";"rotate_right"->"ROTATE_RIGHT";else->null
        }
        val value=when(action.lowercase()) {
            "app_drawer","drawer"->"drawer";"search"->"search";"notifications","expand_notifications"->"notifications"
            "lock","lock_screen"->"screenOff";"recents"->"recents";"widgets"->"widgets";"edit"->"edit";"none"->"none"
            else->if(action.startsWith("app:") && action.removePrefix("app:").matches(Regex("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+"))) action else null
        }
        if(key!=null && value!=null) "gesture_$key" to value else null
    }.toMap()
}
