package com.muwire.gui;

public class HTMLSanitizer {
    private static final String escapeChars[] = {"&", "\"", "<", ">"}; //, "'"}; // apostrophe not supported
    private static final String escapeCodes[] = {"&amp;amp;", "&amp;quot;", "&amp;lt;", "&amp;gt;", "&amp;apos;"};
    private static final String escapedCodes[] = {"&amp;", "&quot;", "&lt;", "&gt;"}; //, "&apos;"}; apostrophe not supported
    
    public static String sanitize(String s) {
        if (s == null)
            return null;
        String escaped = s;
        for (int i = 0; i < escapeChars.length; i++) {
            escaped = escaped.replace(escapeChars[i], escapedCodes[i]);
        }
        return "<html><body>" + escaped + "</body></html>";
    }
}
