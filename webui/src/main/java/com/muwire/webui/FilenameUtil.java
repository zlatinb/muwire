package com.muwire.webui;

import net.i2p.data.DataHelper;

/**
 * File name encoding methods
 *
 * From SusiMail. GPLv2 or any later version.
 */
public class FilenameUtil {

	/**
	 * Convert the UTF-8 to ASCII suitable for inclusion in a header
	 * and for use as a cross-platform filename.
	 * Replace chars likely to be illegal in filenames,
	 * and non-ASCII chars, with _
	 *
	 * Ref: RFC 6266, RFC 5987, i2psnark Storage.ILLEGAL
	 *
	 * @since 0.9.18
	 */
	public static String sanitizeFilename(String name) {
		name = name.trim();
		StringBuilder buf = new StringBuilder(name.length());
		for (int i = 0; i < name.length(); i++) {
			char c = name.charAt(i);
			// illegal filename chars
			if (c <= 32 || c >= 0x7f ||
			    c == '<' || c == '>' || c == ':' || c == '"' ||
			    c == '/' || c == '\\' || c == '|' || c == '?' ||
			    c == '*')
				buf.append('_');
			else
				buf.append(c);
		}
		return buf.toString();
	}

	/**
	 * Encode the UTF-8 suitable for inclusion in a header
	 * as a RFC 5987/6266 filename* value, and for use as a cross-platform filename.
	 * Replace chars likely to be illegal in filenames with _
	 *
	 * Ref: RFC 6266, RFC 5987, i2psnark Storage.ILLEGAL
	 *
	 * This does NOT do multiline, e.g. filename*0* (RFC 2231)
	 *
	 * ref: https://blog.nodemailer.com/2017/01/27/the-mess-that-is-attachment-filenames/
	 * ref: RFC 2231
	 *
	 * @since 0.9.33
	 */
	public static String encodeFilenameRFC5987(String name) {
		name = name.trim();
		StringBuilder buf = new StringBuilder(name.length());
		buf.append("utf-8''");
		for (int i = 0; i < name.length(); i++) {
			char c = name.charAt(i);
			// illegal filename chars
			if (c < 32 || (c >= 0x7f && c <= 0x9f) ||
			    c == '<' || c == '>' || c == ':' || c == '"' ||
			    c == '/' || c == '\\' || c == '|' || c == '?' ||
			    c == '*' ||
			    // unicode newlines
			    c == 0x2028 || c == 0x2029) {
				buf.append('_');
			} else if (c == ' ' || c == '\'' || c == '%' ||          // not in 5987 attr-char
			           c == '(' || c == ')' || c == '@' ||           // 2616 separators
			           c == ',' || c == ';' || c == '[' || c == ']' ||
			           c == '=' || c == '{' || c == '}') {
				// single byte encoding
				buf.append(HexTable.table[c].replace('=', '%'));
			} else if (c < 0x7f) {
				// single byte char, as-is
				buf.append(c);
			} else {
				// multi-byte encoding
				byte[] utf = DataHelper.getUTF8(String.valueOf(c));
				for (int j = 0; j < utf.length; j++) {
					int b = utf[j] & 0xff;
					buf.append(HexTable.table[b].replace('=', '%'));
				}
			}
		}
		return buf.toString();
	}
}
