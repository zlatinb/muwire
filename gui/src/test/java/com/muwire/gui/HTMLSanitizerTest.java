package com.muwire.gui;

import org.junit.Test;

public class HTMLSanitizerTest {
    
    private static void assertSanitized(String raw, String sanitized) {
        assert HTMLSanitizer.sanitize(raw).equals("<html><body>" + sanitized + "</body></html>");
    }
    @Test
    public void testNull() {
        assert HTMLSanitizer.sanitize(null) == null;
    }
    
    @Test
    public void testSingleChars() {
        assertSanitized("&", "&amp;");
        assertSanitized("\"","&quot;");
        assertSanitized("<","&lt;");
        assertSanitized(">","&gt;");
        assertSanitized(" ", "&nbsp;");
    }
    
    @Test
    public void testEvilHTMLDoubleQuote() {
        assertSanitized("<html><img src=\"my.tracking.server.com\"/></html>",
                "&lt;html&gt;&lt;img&nbsp;src=&quot;my.tracking.server.com&quot;/&gt;&lt;/html&gt;");
    }
    
    @Test
    public void textMixture() {
        assertSanitized("><\"&", "&gt;&lt;&quot;&amp;");
    }
}