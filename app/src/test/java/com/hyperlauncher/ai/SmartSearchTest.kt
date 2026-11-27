package com.hyperlauncher.ai
import com.hyperlauncher.ai.ui.search.*
import org.junit.Assert.*
import org.junit.Test
class SmartSearchTest {
    @Test fun calculatorAndConversions() {
        assertEquals("14",LocalAnswers.answer("2+3*4"))
        assertEquals("20",LocalAnswers.answer("(2+3)*4"))
        assertEquals("3.10685596 miles",LocalAnswers.answer("5 km to miles"))
        assertEquals("32 f",LocalAnswers.answer("0 c to f"))
        assertNull(LocalAnswers.answer("1/0")); assertNull(LocalAnswers.answer("java.lang.Runtime()"))
        assertNull(LocalAnswers.answer("1+2 garbage")); assertNull(LocalAnswers.answer("5 kg to km"))
    }
    @Test fun templatesRejectNonHttpsAndEncodeQuery() {
        assertTrue(SearchTemplates.valid("https://example.com/?q={query}"))
        assertFalse(SearchTemplates.valid("javascript:{query}"))
        assertFalse(SearchTemplates.valid("https://user:password@example.com/?q={query}"))
        assertEquals("https://example.com/?q=a%26b",SearchTemplates.resolve("https://example.com/?q={query}","a&b"))
    }
}
