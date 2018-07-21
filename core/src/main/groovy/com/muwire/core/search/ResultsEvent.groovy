package com.muwire.core.files

import com.muwire.core.Event
import com.muwire.core.SharedFile

class ResultsEvent extends Event {

	SharedFile[] results
	UUID uuid

}
