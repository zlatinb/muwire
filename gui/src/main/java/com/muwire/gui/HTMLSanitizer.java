package com.muwire.gui;

public class HTMLSanitizer {
    
    public static String sanitize(String s) {
        if (s == null)
            return null;
        StringBuilder sb = new StringBuilder(s.length() * 2 + 26);
        sb.append("<html><body>");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch(c) {
                case '&': sb.append("&amp;"); break;
                case '\"': sb.append("&quot;"); break;
                case '<' : sb.append("&lt;"); break;
                case '>' : sb.append("&gt;"); break;
                default :
                    sb.append(c);
            }
        }
        sb.append("</body></html>");
        return sb.toString();
    }
}
