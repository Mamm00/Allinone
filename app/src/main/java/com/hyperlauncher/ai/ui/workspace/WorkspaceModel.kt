package com.hyperlauncher.ai.ui.workspace

import kotlinx.serialization.json.*
import java.util.UUID

/** Coordinates are proportions of the usable page, independent of density and insets. */
data class Placement(
    val id: String = UUID.randomUUID().toString(), val page: Int = 0,
    val x: Float = .06f, val y: Float = .1f, val width: Float = .55f, val height: Float = .25f,
    val widgets: List<Int> = emptyList(), val activeWidget: Int = 0,
    val packageName: String = "", val kind: String = "widget"
) {
    fun bounded(): Placement {
        val w = width.takeIf { it.isFinite() }?.coerceIn(.12f, 1f) ?: .55f
        val h = height.takeIf { it.isFinite() }?.coerceIn(.08f, 1f) ?: .25f
        return copy(x = (x.takeIf { it.isFinite() } ?: 0f).coerceIn(0f, 1f-w),
            y = (y.takeIf { it.isFinite() } ?: 0f).coerceIn(0f, 1f-h), width = w, height = h,
            widgets = widgets.distinct().filter { it > 0 }.take(8), page = page.coerceIn(0, 8))
    }
}
data class WorkspaceLayout(val pages: Int = 3, val columns: Int = 4, val rows: Int = 6,
    val gridless: Boolean = true, val items: List<Placement> = emptyList())

object CircularPages {
    fun wrap(index: Int, count: Int): Int { require(count > 0); return Math.floorMod(index, count) }
    fun shortestDelta(from: Int, to: Int, count: Int): Int {
        val forward = wrap(to - from, count)
        return if (forward <= count / 2) forward else forward - count
    }
    fun logical(virtual: Int, count: Int): Int = wrap(virtual, count)
    fun initial(count: Int): Int = Int.MAX_VALUE / 2 - (Int.MAX_VALUE / 2) % count
}

object WorkspaceCodec {
    fun encode(layout: WorkspaceLayout): String = buildJsonObject {
        put("pages", layout.pages); put("columns", layout.columns); put("rows", layout.rows)
        put("gridless", layout.gridless)
        putJsonArray("items") { layout.items.forEach { item -> add(buildJsonObject {
            put("id", item.id); put("page", item.page); put("x", item.x); put("y", item.y)
            put("width", item.width); put("height", item.height); put("active", item.activeWidget)
            put("package", item.packageName); put("kind", item.kind)
            putJsonArray("widgets") { item.widgets.forEach { add(it) } }
        }) } }
    }.toString()
    fun decode(text: String): WorkspaceLayout {
        val root = Json.parseToJsonElement(text).jsonObject
        fun int(key: String, default: Int) = root[key]?.jsonPrimitive?.intOrNull ?: default
        return WorkspaceLayout(int("pages",3).coerceIn(1,9), int("columns",4).coerceIn(2,10),
            int("rows",6).coerceIn(2,12), root["gridless"]?.jsonPrimitive?.booleanOrNull ?: true,
            root["items"]?.jsonArray.orEmpty().take(64).map { raw ->
                val i = raw.jsonObject
                fun f(key: String, default: Float) = i[key]?.jsonPrimitive?.floatOrNull ?: default
                Placement(id = i["id"]?.jsonPrimitive?.content ?: UUID.randomUUID().toString(),
                    page = i["page"]?.jsonPrimitive?.intOrNull ?: 0,
                    x=f("x",.06f), y=f("y",.1f), width=f("width",.55f), height=f("height",.25f),
                    widgets=i["widgets"]?.jsonArray.orEmpty().mapNotNull { it.jsonPrimitive.intOrNull },
                    activeWidget=i["active"]?.jsonPrimitive?.intOrNull ?: 0,
                    packageName=i["package"]?.jsonPrimitive?.content.orEmpty(),
                    kind=i["kind"]?.jsonPrimitive?.content ?: "widget").bounded()
            }.distinctBy { it.id })
    }
}
