package com.muwire.core;

import net.i2p.crypto.SigType;

public class Constants {
    public static final byte PERSONA_VERSION = (byte)1;
    public static final String INVALID_NICKNAME_CHARS = "'\"();<>=@$%";
    public static final int MAX_NICKNAME_LENGTH = 30;
    
    public static final byte PROFILE_HEADER_VERSION = (byte)1;
    public static final int MAX_PROFILE_TITLE_LENGTH = 128;
    
    public static final byte PROFILE_VERSION = (byte)1;
    public static final int MAX_PROFILE_IMAGE_LENGTH = 200 * 1024;
    public static final int MAX_PROFILE_LENGTH = 0x1 << 18;
    
    public static final byte FILE_CERT_VERSION = (byte)2;
    public static final int CHAT_VERSION = 2;
    
    public static final byte COLLECTION_VERSION = (byte)1;
    public static final byte COLLECTION_ENTRY_VERSION = (byte)1;
    public static final int COLLECTION_MAX_ITEMS = (0x1 << 16) - 1;
    
    public static final byte MESSENGER_MESSAGE_VERSION = (byte)1;
    public static final byte MESSENGER_ATTACHMENT_VERSION = (byte)1;
    
    public static final SigType SIG_TYPE = SigType.EdDSA_SHA512_Ed25519;

    public static final int MAX_HEADER_SIZE = 0x1 << 14;
    public static final int MAX_HEADERS = 16;
    public static final long MAX_HEADER_TIME = 60 * 1000;
    
    public static final int MAX_RESULTS = 0x1 << 20;
    
    public static final int MAX_COMMENT_LENGTH = 0x1 << 15;
    
    public static final long MAX_QUERY_AGE = 5 * 60 * 1000L;
    
    public static final int UPDATE_PORT = 2;
    public static final int TRACKER_PORT = 3;
}
