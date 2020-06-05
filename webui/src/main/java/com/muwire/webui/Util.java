package com.muwire.webui;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import groovy.json.JsonOutput;
import net.i2p.I2PAppContext;
import net.i2p.data.Base64;
import net.i2p.data.DataHelper;
import net.i2p.util.Translate;

public class Util {
    
    static void pause() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {}
    }

    private static final I2PAppContext _context = I2PAppContext.getGlobalContext();
    
    private static final String escapeChars[] = {"&", "\"", "<", ">", "'"};
    private static final String escapeCodes[] = {"&amp;amp;", "&amp;quot;", "&amp;lt;", "&amp;gt;", "&amp;apos;"};
    private static final String escapedCodes[] = {"&amp;", "&quot;", "&lt;", "&gt;", "&apos;"};

    // if we had a lot of these we could scan for them in the build and generate
    // a file, but it's not worth it for just a handful.
    private static final String[] jsStrings = {
        // alphabetical please
        _x("About"),
        _x("About Me"),
        _x("Actions"),
        _x("Active Sources"),
        _x("Browse"),
        _x("Browsing"),
        _x("Cancel"),
        _x("Certificates"),
        _x("Certified"),
        _x("Certify"),
        _x("Clear Finished"),
        _x("Comment"),
        _x("Copy hash to clipboard"),
        _x("Copy To Clipbaord"),
        _x("Default settings for new feeds"),
        _x("Details for {0}"),
        _x("Directory configuration for {0}"),
        _x("Directory sync frequency (seconds, 0 means never)"),
        _x("Distrusted"),
        _x("Distrusted User"),
        _x("Down"),
        _x("Download"),
        _x("Download each file sequentially"),
        _x("Download Location"),
        _x("Download published files automatically"),
        _x("Downloaded"),
        _x("Downloaded Pieces"),
        _x("Downloader"),
        _x("Downloading"),
        _x("Enter Reason (Optional)"),
        _x("ETA"),
        _x("Failing Hosts"),
        _x("Feed configuration for {0}"),
        _x("Feed update frequency (minutes)"),
        _x("Feeds"),
        _x("Fetching Certificates"),
        _x("Feed"),
        _x("File"),
        _x("Files"),
        _x("Hash copied to clipboard"),
        _x("Hashing"),
        _x("Hide Certificates"),
        _x("Hide Comment"),
        _x("Hopeless Hosts"),
        _x("Host"),
        _x("Import"),
        _x("Imported"),
        _x("Incoming Connections"),
        _x("Known Hosts"),
        _x("Known Sources"),
        _x("Last Updated"),
        _x("Mark Distrusted"),
        _x("Mark Neutral"),
        _x("Mark Trusted"),
        _x("Monitor directory for changes"),
        _x("MuWire Status"),
        _x("must be greater than zero"),
        _x("Name"),
        _x("Never"),
        _x("Number of items to keep on disk (-1 means unlimited)"),
        _x("Outgoing Connections"),
        // verb
        _x("Pause"),
        _x("Piece Size"),
        _x("Progress"),
        _x("Publish"),
        _x("Publish shared files automatically"),
        _x("Published"),
        _x("Publisher"),
        _x("Publishing"),
        _x("Query"),
        // noun
        _x("Reason"),
        _x("Refresh"),
        _x("Remote Pieces"),
        _x("Results"),
        _x("Results for {0}"),
        _x("Results from {0}"),
        _x("Resume"),
        _x("Retry"),
        _x("Save"),
        _x("Search"),
        _x("Searcher"),
        _x("Sender"),
        _x("Senders"),
        _x("Shared Files"),
        _x("Sharing"),
        _x("Show Comment"),
        _x("Show Details"),
        _x("Size"),
        _x("Sources"),
        _x("Speed"),
        _x("State"),
        _x("Status"),
        _x("Submit"),
        _x("Subscribe"),
        _x("Subscribed"),
        _x("Sync"),
        _x("Times Browsed"),
        _x("Timestamp"),
        _x("Total Pieces"),
        _x("Trust"),
        _x("Trusted"),
        _x("Trusted User"),
        _x("Unpublish"),
        _x("Unshare"),
        _x("Unsubscribe"),
        _x("Update"),
        _x("Upload"),
        _x("Uploads"),
        _x("User"),
        _x("View 1 Certificate"),
        _x("View {0} Certificates"),
        _x("Your Trust"),
        _x("{0}% of piece")
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
    
    public static File getFromPathElements(String pathElements) {
        File current = null;
        for (String element : DataHelper.split(pathElements,",")) {
            element = unescapeHTMLinXML(Base64.decodeToString(element));
            if (element == null) {
                return null;
            }
            if (current == null)
                current = new File(element);
            else 
                current = new File(current, element);
        }
        return current;
    }

    /**
     * @return a JSON-encoded mapping of translations needed for the javascript,
     *         HTML-escaped.
     */
    public static String getJSTranslations() {
        if (Translate.getLanguage(_context).equals("en"))
            return "{}";
        Map<String, String> map = new HashMap<String, String>(jsStrings.length);
        for (String s : jsStrings) {
            String tx = _t(s);
            if (!s.equals(tx))
                map.put(s, tx);
        }
        return JsonOutput.toJson(map);
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
