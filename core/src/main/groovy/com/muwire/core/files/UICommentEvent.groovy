package com.muwire.core.files

import com.muwire.core.Event
import com.muwire.core.SharedFile

class UICommentEvent extends Event {
    SharedFile sharedFile
    String oldComment
}
