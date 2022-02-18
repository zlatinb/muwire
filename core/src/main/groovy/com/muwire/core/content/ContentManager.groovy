package com.muwire.core.content

import com.muwire.core.EventBus
import com.muwire.core.MuWireSettings
import com.muwire.core.trust.TrustEvent
import com.muwire.core.trust.TrustLevel
import com.muwire.core.util.DataUtil
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.util.logging.Log

import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap

import com.muwire.core.search.QueryEvent

import net.i2p.util.ConcurrentHashSet
import net.i2p.data.Base64

@Log
class ContentManager {

    private final MuWireSettings settings
    private final File rulesHome
    private final EventBus eventBus
    
    ContentManager(EventBus eventBus, File home, MuWireSettings settings) {
        this.eventBus = eventBus
        this.settings = settings
        rulesHome = new File(home,"rules")
    }
    
    Set<Matcher> matchers = new ConcurrentHashSet()
    
    void start() {
        
        if (!rulesHome.exists()) {
            rulesHome.mkdir()

            settings.watchedKeywords.each {
                def event = new ContentControlEvent(term: it, add: true, 
                        action: MatchAction.RECORD, name: "keyword-$it")
                onContentControlEvent(event)
            }
            settings.watchedKeywords.clear()

            settings.watchedRegexes.each {
                String encoded = Base64.encode(DataUtil.encodei18nString(it))
                def event = new ContentControlEvent(term: it, regex: true, add: true, 
                        action: MatchAction.RECORD, name: "regex-$encoded")
                onContentControlEvent(event)
            }
            settings.watchedRegexes.clear()

            log.info("finished converting old rules")
        } else {
            def slurper = new JsonSlurper()
            for (File ruleFile : rulesHome.listFiles()) {
                if (!ruleFile.getName().endsWith("json"))
                    continue
                
                def json = slurper.parse(ruleFile, "UTF-8")
                Matcher m
                MatchAction action = MatchAction.valueOf(json.action)
                if (json.regex) {
                    m = new RegexMatcher(json.term, action, json.name)
                } else {
                    m = new KeywordMatcher(json.term, action, json.name)
                }
                matchers << m    
            }
        }
    }
    
    void onContentControlEvent(ContentControlEvent e) {
        
        if (e.add) {
            File ruleFile = new File(rulesHome, "${e.name}.json")
            Matcher m
            if (e.regex)
                m = new RegexMatcher(e.term, e.action, e.name)
            else
                m = new KeywordMatcher(e.term, e.action, e.name)
            matchers.add(m)
            
            def json = [:]
            json.name = e.name
            json.action = e.action.name()
            json.regex = e.regex
            json.term = e.term
            
            String serialized = JsonOutput.toJson(json)
            ruleFile.withPrintWriter("UTF-8", {it.println(serialized)})
        } else {
            matchers.remove(e.matcher)
            File ruleFile = new File(rulesHome, "${e.matcher.name}.json")
            ruleFile.delete()
        }
    }
    
    void onQueryEvent(QueryEvent e) {
        if (e.searchEvent.searchTerms == null)
            return
        for (Matcher matcher : matchers) {
            MatchAction action = matcher.process(e)
            if (action == null)
                continue
            switch(action) {
                case MatchAction.BLOCK:
                    log.info("blocking $e")
                    def event = new TrustEvent(
                            persona: e.originator,
                            level: TrustLevel.DISTRUSTED,
                            reason: "Blocked by rule ${matcher.name}")
                    eventBus.publish event
                case MatchAction.DROP: 
                    log.info("vetoing $e")
                    e.vetoed = true
                default:
                    break
            }
        }
    }
}
