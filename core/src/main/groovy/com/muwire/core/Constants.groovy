package com.muwire.core

import net.i2p.crypto.SigType

class Constants {
    public static final byte PERSONA_VERSION = (byte)1
    public static final SigType SIG_TYPE = SigType.ECDSA_SHA512_P521 // TODO: decide which
    
    public static final int MAX_HEADER_SIZE = 0x1 << 14
    public static final int MAX_HEADERS = 16
}
