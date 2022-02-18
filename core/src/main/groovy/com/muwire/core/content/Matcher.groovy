package com.muwire.core.content

import com.muwire.core.search.QueryEvent

abstract class Matcher {
    final List<Match> matches = Collections.synchronizedList(new ArrayList<>())
    final Set<UUID> uuids = new HashSet<>()
    private final MatchAction action
    final String name
    
    Matcher(MatchAction action, String name) {
        this.action = action
        this.name = name
    }
    
    protected abstract boolean match(List<String> searchTerms);
    
    public abstract String getTerm();
    
    public MatchAction process(QueryEvent qe) {
        def terms = qe.searchEvent.searchTerms
        if (!match(terms))
            return null
        if (uuids.add(qe.searchEvent.uuid)) {
            long now = System.currentTimeMillis()
            matches << new Match(persona : qe.originator, keywords : terms, timestamp : now)
        }
        action
    }
}
