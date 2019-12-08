package com.muwire.webui;

import net.i2p.I2PAppContext;
import net.i2p.util.Translate;

public class Util {

    private static final I2PAppContext _context = I2PAppContext.getGlobalContext();
    
    private static final String escapeChars[] = {"&", "\"", "<", ">", "'"};
    private static final String escapeCodes[] = {"&amp;amp;", "&amp;quot;", "&amp;lt;", "&amp;gt;", "&amp;apos;"};
    private static final String escapedCodes[] = {"&amp;", "&quot;", "&lt;", "&gt;", "&apos;"};

    private static final String BUNDLE_NAME = "com.muwire.webui.messages";

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
    
    public static String unescapeHTMLinXML(String escaped) {
        if (escaped == null) return null;
        String unescaped = escaped;
        for (int i = 0; i < escapedCodes.length; i++) {
            unescaped = unescaped.replace(escapedCodes[i], escapeChars[i]);
        }
        return unescaped;
    }

    /** translate a string */
    public static String _t(String s) {
        return Translate.getString(s, _context, BUNDLE_NAME);
    }

    /**
     *  translate a string with a parameter
     *  This is a lot more expensive than _t(s), so use sparingly.
     *
     *  @param s string to be translated containing {0}
     *    The {0} will be replaced by the parameter.
     *    Single quotes must be doubled, i.e. ' -&gt; '' in the string.
     *  @param o parameter, not translated.
     *    To translate parameter also, use _t("foo {0} bar", _t("baz"))
     *    Do not double the single quotes in the parameter.
     *    Use autoboxing to call with ints, longs, floats, etc.
     */
    public static String _t(String s, Object o) {
        return Translate.getString(s, o, _context, BUNDLE_NAME);
    }

    /** two params @since 0.7.14 */
    public static String _t(String s, Object o, Object o2) {
        return Translate.getString(s, o, o2, _context, BUNDLE_NAME);
    }

    /** translate (ngettext) @since 0.7.14 */
    public static String ngettext(String s, String p, int n) {
        return Translate.getString(n, s, p, _context, BUNDLE_NAME);
    }

    /**
     *  Mark a string for extraction by xgettext and translation.
     *  Use this only in static initializers.
     *  It does not translate!
     *  @return s
     */
    public static String _x(String s) {
        return s;
    }
}
