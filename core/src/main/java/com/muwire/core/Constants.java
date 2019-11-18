package com.muwire.core;

import net.i2p.crypto.SigType;

public class Constants {
    public static final byte PERSONA_VERSION = (byte)1;
    public static final byte FILE_CERT_VERSION = (byte)2;
    public static final int CHAT_VERSION = 1;
    
    public static final SigType SIG_TYPE = SigType.EdDSA_SHA512_Ed25519;

    public static final int MAX_HEADER_SIZE = 0x1 << 14;
    public static final int MAX_HEADERS = 16;
    public static final long MAX_HEADER_TIME = 60 * 1000;
    
    public static final int MAX_RESULTS = 0x1 << 16;
    
    public static final int MAX_COMMENT_LENGTH = 0x1 << 15;
    
    public static final long MAX_QUERY_AGE = 5 * 60 * 1000L; 
}
