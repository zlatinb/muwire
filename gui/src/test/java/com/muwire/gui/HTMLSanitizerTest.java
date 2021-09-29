package com.muwire.gui;

import org.junit.Test;

public class HTMLSanitizerTest {
    
    private static void assertSantizied(String raw, String sanitized) {
        assert HTMLSanitizer.sanitize(raw).equals("<html><body>" + sanitized + "</body></html>");
    }
    @Test
    public void testNull() {
        assert HTMLSanitizer.sanitize(null) == null;
    }
    
    @Test
    public void testSingleChars() {
        assertSantizied("&", "&amp;");
        assertSantizied("\"","&quot;");
        assertSantizied("<","&lt;");
        assertSantizied(">","&gt;");
    }
    
    @Test
    public void testEvilHTMLDoubleQuote() {
        assertSantizied("<html><img src=\"my.tracking.server.com\"/></html>",
                "&lt;html&gt;&lt;img src=&quot;my.tracking.server.com&quot;/&gt;&lt;/html&gt;");
    }
    
    @Test
    public void textMixture() {
        assertSantizied("><\"&", "&gt;&lt;&quot;&amp;");
    }
}