package com.muwire.webui;

import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONObject;

import net.i2p.I2PAppContext;
import net.i2p.data.DataHelper;
import net.i2p.util.Translate;

public class Util {

    private static final I2PAppContext _context = I2PAppContext.getGlobalContext();
    
    private static final String escapeChars[] = {"&", "\"", "<", ">", "'"};
    private static final String escapeCodes[] = {"&amp;amp;", "&amp;quot;", "&amp;lt;", "&amp;gt;", "&amp;apos;"};
    private static final String escapedCodes[] = {"&amp;", "&quot;", "&lt;", "&gt;", "&apos;"};

    // if we had a lot of these we could scan for them in the build and generate
    // a file, but it's not worth it for just a handful.
    private static final String[] jsStrings = {
        _x("View Certificates"),
        _x("Import"),
        _x("Imported"),
        _x("Unsubscribe"),
        _x("Actions"),
        _x("Browse"),
        _x("Browsing"),
        _x("Cancel"),
        _x("Details For {0}"),
        _x("Down"),
        _x("Download"),
        _x("Downloading"),
        _x("Enter Reason (Optional)"),
        _x("ETA"),
        _x("File"),
        _x("Hide Comment"),
        _x("Host"),
        _x("Last Updated"),
        _x("Mark Distrusted"),
        _x("Mark Neutral"),
        _x("Mark Trusted"),
        _x("Name"),
        _x("Progress"),
        _x("Query"),
        _x("Reason"),
        _x("Refresh"),
        _x("Results"),
        _x("Results From {0}"),
        _x("Save"),
        _x("Search"),
        _x("Sender"),
        _x("Senders"),
        _x("Show Comment"),
        _x("Size"),
        _x("Speed"),
        _x("State"),
        _x("Status"),
        _x("Subscribe"),
        _x("Subscribed"),
        _x("Trust"),
        _x("User"),
        _x("Your Trust"),
    };

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

    /**
     * @return a JSON-encoded mapping of translations needed for the javascript,
     *         HTML-escaped.
     */
    public static String getJSTranslations() {
        Map<String, String> map = new HashMap<String, String>(jsStrings.length);
        for (String s : jsStrings) {
            map.put(s, _t(s));
        }
        return JSONObject.toJSONString(map);
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
