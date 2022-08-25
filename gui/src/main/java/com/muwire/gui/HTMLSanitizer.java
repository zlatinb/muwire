package com.muwire.gui;

public class HTMLSanitizer {
    
    public static String sanitize(String s) {
        if (s == null)
            return null;
        StringBuilder sb = new StringBuilder(s.length() * 2 + 26);
        sb.append("<html><body>");
        sb.append(escape(s));
        sb.append("</body></html>");
        return sb.toString();
    }
    
    public static String escape(String s) {
        if (s == null)
            return null;
        StringBuilder sb = new StringBuilder(s.length() * 2);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch(c) {
                case '&': sb.append("&amp;"); break;
                case '\"': sb.append("&quot;"); break;
                case '<' : sb.append("&lt;"); break;
                case '>' : sb.append("&gt;"); break;
                case ' ' : sb.append("&nbsp;"); break;
                default :
                    sb.append(c);
            }
        }
        return sb.toString();
    }
}
