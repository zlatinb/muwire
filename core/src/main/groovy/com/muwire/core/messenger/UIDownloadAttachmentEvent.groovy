package com.muwire.core.messenger

import com.muwire.core.Event
import com.muwire.core.Persona

class UIDownloadAttachmentEvent extends Event {
    Persona sender
    MWMessageAttachment attachment
    boolean sequential
}
