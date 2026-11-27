package com.hyperlauncher.importer
import org.junit.Assert.*
import org.junit.Test
class NovaXmlParserTest {
    @Test fun readsPagesDockFoldersAndWidgetGeometry() {
        val result=NovaXmlParser.parse("""<favorites columns="4" rows="6"><favorite _id="1" intent="#Intent;component=com.example/.Main;end" screen="2" cellX="1" cellY="3"/><folder _id="2"/><appwidget _id="3" spanX="2" spanY="3"/><favorite container="-101" packageName="com.dock"/></favorites>""")
        assertEquals("com.example",result.items[0].packageName)
        assertEquals(.25f,result.items[0].x,.001f);assertEquals(.5f,result.items[0].y,.001f)
        assertEquals("widget",result.items[2].kind);assertEquals(listOf("com.dock"),result.dock)
    }
    @Test(expected=IllegalArgumentException::class) fun rejectsEntities() {NovaXmlParser.parse("<!DOCTYPE foo><favorites/>")}
    @Test(expected=IllegalArgumentException::class) fun rejectsUnrecognizedFormat() {NovaXmlParser.parse("<unrelated/>")}
}
