package com.hyperlauncher.feed
import org.junit.Assert.*
import org.junit.Test
class FeedParserTest {
    @Test fun rssAndAtomResolveOnlyHttpsLinks() {
        val rss="""<rss><channel><item><title>Hello</title><link>/article</link><pubDate>Today</pubDate></item><item><title>Unsafe</title><link>javascript:alert(1)</link></item></channel></rss>"""
        assertEquals(listOf(Article("https://example.com/article","Hello","Today")),FeedParser.parse(rss,"https://example.com/feed"))
        val atom="""<feed xmlns="http://www.w3.org/2005/Atom"><entry><title>Atom</title><link rel="alternate" href="https://example.com/atom"/></entry></feed>"""
        assertEquals("Atom",FeedParser.parse(atom,"https://example.com/feed").single().title)
    }
    @Test(expected=IllegalArgumentException::class) fun rejectsEntityExpansion() {
        FeedParser.parse("<!DOCTYPE x [<!ENTITY y SYSTEM 'file:///etc/passwd'>]><rss/>","https://example.com")
    }
}
