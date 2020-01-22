package com.muwire.core.files

import com.muwire.core.Event

/**
 * Should be triggered by the old PersisterService
 * once it has finished reading the old file
 *
 * @see PersisterService
 */
class PersisterDoneEvent extends Event{
}
