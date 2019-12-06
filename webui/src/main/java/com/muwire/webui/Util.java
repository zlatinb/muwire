package com.muwire.webui;

public class Util {
    
    private static final String escapeChars[] = {"&", "\"", "<", ">", "'"};
    private static final String escapeCodes[] = {"&amp;amp;", "&amp;quot;", "&amp;lt;", "&amp;gt;", "&amp;apos;"};

    /**
     * Double-Escape an HTML string for inclusion in XML
     * @param unescaped the unescaped string, may be null
     * @return the escaped string, or null if null is passed in
     */
    public static String escapeHTMLinXML(String unescaped) {
        if (unescaped == null) return null;
        String escaped = unescaped;
        for (int i = 0; i < escapeChars.length; i++) {
            escaped = escaped.replace(escapeChars[i], escapeCodes[i]);
        }
        return escaped;
    }
}
