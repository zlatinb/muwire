package com.muwire.core.filefeeds

import com.muwire.core.Event

/**
 * Emitted when configuration of a feed changes.  
 * The object should already contain the updated values.
 */
class UIFeedConfigurationEvent extends Event {
    Feed feed
    boolean newFeed
}
