package com.muwire.core.search

import com.muwire.core.Event
import com.muwire.core.SharedFile

class ResultsEvent extends Event {

    SearchEvent searchEvent
	SharedFile[] results
	UUID uuid
}
