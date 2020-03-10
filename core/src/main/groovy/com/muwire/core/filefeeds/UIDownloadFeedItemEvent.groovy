package com.muwire.core.filefeeds

import com.muwire.core.Event

class UIDownloadFeedItemEvent extends Event {
    FeedItem item
    File target
    boolean sequential
}
