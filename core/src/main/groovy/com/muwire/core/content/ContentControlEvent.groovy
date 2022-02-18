package com.muwire.core.content

import com.muwire.core.Event

class ContentControlEvent extends Event {
    String term
    boolean regex
    MatchAction action
    String name
    boolean add
    Matcher matcher
}
