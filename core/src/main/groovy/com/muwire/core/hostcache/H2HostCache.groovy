package com.muwire.core.hostcache

import java.util.function.Predicate

import com.muwire.core.MuWireSettings
import com.muwire.core.connection.ConnectionAttemptStatus
import com.muwire.core.trust.TrustService

import net.i2p.data.Destination
import groovy.json.JsonSlurper
import groovy.sql.Sql
import groovy.util.logging.Log
@Log
class H2HostCache extends HostCache {
    
    private Sql sql
    private final File h2Home
    private final File home
    
    /** contains only hosts to whom there have been no connection attempts at all */
    private final Set<Destination> hosts = new HashSet<>()
    
    /** contains hosts for whom we have computed probability of success */
    private final Map<Destination, RankedHost> rankedHosts = new HashMap<>()
    
    public H2HostCache(File home, TrustService trustService, MuWireSettings settings, Destination myself) {
        super(trustService, settings, myself)
        h2Home = new File(home, "h2")
        this.home = home
    }
    @Override
    protected synchronized void hostDiscovered(Destination d, boolean fromHostcache) {
        if (fromHostcache) { 
            sql.execute("delete from HOST_ATTEMPTS where DESTINATION='${d.toBase64()}';")
            hosts.add(d)
        } else {
            if (sql.rows("select * from HOST_ATTEMPTS where DESTINATION='${d.toBase64()}'").size() == 0)
                hosts.add(d)
        }
    }
    
    @Override
    protected synchronized void onConnection(Destination d, ConnectionAttemptStatus status) {
        // remove from hosts
        hosts.remove(d)
        
        // record into db
        def timestamp = new java.sql.Date(System.currentTimeMillis())
        sql.execute("insert into HOST_ATTEMPTS values ('${d.toBase64()}', '$timestamp', '${status.name()}');")
        
        // and re-rank
        rankedHosts.put(d, rankHost(d))
    }
    @Override
    public synchronized List<Destination> getHosts(int n, Predicate<Destination> filter) {
        List<Destination> rv = getTopHosts(n, filter)
        
        log.fine("got ${rv.size()} ranked hosts out of $n requested")
        
        Iterator<Destination> iter = hosts.iterator()
        while (rv.size() < n && iter.hasNext()) {
            Destination host = iter.next()
            if (filter.test(host))
                rv.add(host)
        }
        
        log.fine("will return total of ${rv.size()} hosts")
        return rv;
    }
    @Override
    public synchronized List<Destination> getGoodHosts(int n) {
        // TODO look into DB and give a random sample of successful hosts
        return Collections.emptyList();
    }
    @Override
    public synchronized int countFailingHosts() {
        // TODO count from db
        return 0;
    }
    @Override
    public synchronized int countHopelessHosts() {
        // TODO count from db
        return 0;
    }
    @Override
    public synchronized void start() {
        def db = [ url : "jdbc:h2:" + h2Home.getAbsolutePath(),
            user : "muwire",
            password : "",
            driver : "org.h2.Driver" ]
        sql = Sql.newInstance(db.url, db.user, db.password, db.driver)
        
        boolean success = sql.execute("CREATE TABLE IF NOT EXISTS HOST_ATTEMPTS(DESTINATION VARCHAR(1024), TSTAMP TIMESTAMP," +
                    "STATUS ENUM('SUCCESSFUL','REJECTED','FAILED'))")
        log.info("created table $success")
        
        Thread loader = new Thread({ load() } as Runnable )
        loader.start()
        
    }
    @Override
    public synchronized void stop() {
        sql.close()
    }
    @Override
    public synchronized void load() {
        // 1. see if hosts.json still exists, assume everyone is good to connect
        File hostsJson = new File(home, "hosts.json")
        if (hostsJson.exists()) {
            log.info("migrating old hosts.json")
            def slurper = new JsonSlurper()
            hostsJson.eachLine { 
                def entry = slurper.parseText(it)
                Destination dest = new Destination(entry.destination)
                long timestamp = System.currentTimeMillis()
                if (entry.lastSuccessfulAttempt != null)
                    timestamp = entry.lastSuccessfulAttempt
                def tstamp = new java.sql.Date(timestamp)
                sql.execute("insert into HOST_ATTEMPTS VALUES ('${dest.toBase64()}', '$tstamp', 'SUCCESSFUL')")
            }
            hostsJson.renameTo(new File(home, "hosts.json.bak"))
            log.info("migrated hosts.json")
        }
        
        // 2. load each host from DB and rank
        log.info("loading hosts from db")
        sql.eachRow("select distinct DESTINATION from HOST_ATTEMPTS") { 
            Destination dest = new Destination(it.DESTINATION)
            RankedHost rankedHost = rankHost(dest)
            rankedHosts.put(dest, rankedHost)
        }
        log.info("loaded ${rankedHosts.size()} hosts")
        loaded = true
    }
    
    private List<Destination> getTopHosts(int n, Predicate<Destination> filter) {
        List<RankedHost> ranked = new ArrayList<>(rankedHosts.values())
        ranked.sort({l,r -> Double.compare(r.probability, l.probability)})
        
        log.fine("before filtering there are ${ranked.size()} ranked hosts")
        ranked.retainAll {
            filter.test(it.destination)
        }
        log.fine("after filtering there are ${ranked.size()} ranked hosts")
        
        if (ranked.size() > n)
            ranked = ranked[0..n-1]
        ranked.collect { it.destination }
    }
    
    private RankedHost rankHost(Destination d) {
        // TODO: load connection history for the host from the DB
        // then use ML to compute probability of success.
        
        // until that is implemented, assume everyone has good chances
        return new RankedHost(d, 1.0d)
    }
    
    private static class RankedHost {
        private final Destination destination
        private final double probability
        RankedHost(Destination destination, double probability) {
            this.destination = destination
            this.probability = probability
        }
    }
}