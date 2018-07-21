package com.muwire.core.search

import com.muwire.core.Event

class SearchEvent extends Event {

	List<String> searchTerms
	byte [] searchHash
	UUID uuid
}
