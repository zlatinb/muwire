package com.muwire.core.upload

import com.muwire.core.Event
import com.muwire.core.profile.MWProfileHeader

public class UploadEvent extends Event {
    Uploader uploader
    boolean first
    MWProfileHeader profileHeader
}
