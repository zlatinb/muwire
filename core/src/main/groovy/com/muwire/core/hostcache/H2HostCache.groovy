package com.muwire.core.hostcache

import java.util.function.Predicate
import java.util.function.Supplier

import java.text.SimpleDateFormat

import com.muwire.core.MuWireSettings
import com.muwire.core.connection.ConnectionAttemptStatus
import com.muwire.core.trust.TrustService

import net.i2p.data.Destination
import groovy.json.JsonSlurper
import groovy.sql.Sql
import groovy.util.logging.Log
@Log
class H2HostCache extends HostCache {
    
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
    private Sql sql
    private final File h2Home
    private final File home
    
    private Supplier<Collection<Destination>> connSupplier

    private final List<Destination> allHosts = new ArrayList<>()
    private final Set<Destination> uniqueHosts = new HashSet<>()
    private final Map<Destination, HostMCProfile> profiles = new HashMap<>()    
    
    private final Timer timer
    
    private Collection<Destination> toVerify = new LinkedHashSet<>()
    
    public H2HostCache(File home, TrustService trustService, MuWireSettings settings, Destination myself) {
        super(trustService, settings, myself)
        h2Home = new File(home, "h2")
        this.home = home
        this.timer = new Timer("host-persister", true)
    }
    
    @Override
    protected synchronized void hostDiscovered(Destination d, boolean fromHostcache) {
        // overwrite MC with optimistic values 
        if (fromHostcache) {
            sql.execute("delete from HOST_ATTEMPTS where DESTINATION='${d.toBase64()}';")
            profiles.put(d, new HostMCProfile())
        } else if (uniqueHosts.add(d)) {
            allHosts.add(d)
            profiles.put(d, new HostMCProfile())
        }
    }
    
    @Override
    protected synchronized void onConnection(Destination d, ConnectionAttemptStatus status) {
        
        // record into db
        def timestamp = new Date(System.currentTimeMillis())
        timestamp = SDF.format(timestamp)
        sql.execute("insert into HOST_ATTEMPTS values ('${d.toBase64()}', '$timestamp', '${status.name()}');")
        
        def count = sql.firstRow("select count(*) as COUNT from HOST_ATTEMPTS where DESTINATION=${d.toBase64()}")
        if (count.COUNT < settings.hostProfileHistory) 
            return
       
        log.fine("recomputing Markov for ${d.toBase32()}")
        
        int ss = 0
        int sr = 0
        int sf = 0
        int rs = 0
        int rr = 0
        int rf = 0
        int fs = 0
        int fr = 0
        int ff = 0
        ConnectionAttemptStatus currentStatus = ConnectionAttemptStatus.SUCCESSFUL
        
        sql.eachRow("select STATUS from HOST_PROFILES where DESTINATION=${d.toBase64()} ORDER BY TSTAMP") {  
            def recordedStatus = ConnectionAttemptStatus.valueOf(it.STATUS)
            switch(currentStatus) {
                case ConnectionAttemptStatus.SUCCESSFUL:
                switch(recordedStatus) {
                    case ConnectionAttemptStatus.SUCCESSFUL: ss++; break;
                    case ConnectionAttemptStatus.REJECTED: sr++; break;
                    case ConnectionAttemptStatus.FAILED: sf++; break;
                }
                break
                case ConnectionAttemptStatus.REJECTED:
                switch(recordedStatus) {
                    case ConnectionAttemptStatus.SUCCESSFUL: rs++; break;
                    case ConnectionAttemptStatus.REJECTED: rr++; break;
                    case ConnectionAttemptStatus.FAILED: rf++; break;
                }
                break
                case ConnectionAttemptStatus.FAILED:
                switch(recordedStatus) {
                    case ConnectionAttemptStatus.SUCCESSFUL: fs++; break;
                    case ConnectionAttemptStatus.REJECTED: fr++; break;
                    case ConnectionAttemptStatus.FAILED: ff++; break;
                }
                break
            }
            currentStatus = recordedStatus
        }

        sql.execute("delete from HOST_PROFILES where DESTINATION=${d.toBase64()}")
        sql.execute("insert into HOST_PROFILES values (" +
            "${d.toBase64()}," +
            "${ss * 1.0d / count}," +
            "${sr * 1.0d / count}," +
            "${sf * 1.0d / count}," +
            "${rs * 1.0d / count}," +
            "${rr * 1.0d / count}," +
            "${rf * 1.0d / count}," +
            "${fs * 1.0d / count}," +
            "${fr * 1.0d / count}," +
            "${ff * 1.0d / count}," +
            ")")
        def newProfile = sql.firstRow("select * from HOST_PROFILES where DESTINATION=${d.toBase64()}")
        profiles.put(d, new HostMCProfile(newProfile))
        log.fine("profile updated")        
    }
    
    @Override
    public synchronized List<Destination> getHosts(int n, Predicate<Destination> filter) {
        if (allHosts.isEmpty())
            return Collections.emptyList()
        
        List<Destination> rv = new ArrayList<>()
        Iterator<Destination> verifyIter = toVerify.iterator()
        while((rv.size() < n) && (verifyIter.hasNext())) {
            rv.add(verifyIter.next())
            verifyIter.remove()
        }
        
        log.fine("got ${rv.size()} from toVerify list")
        
        if (rv.size() == n)
            return rv

        Set<Destination> cannotTry = new HashSet<>()                
        while(rv.size() < n && cannotTry.size() < allHosts.size()) {
            Destination d = allHosts.get((int)(Math.random() * allHosts.size()))
            if (cannotTry.contains(d))
                continue
            if (!filter.test(d)) {
                cannotTry.add(d)
                continue
            }
                
            HostMCProfile profile = profiles.get(d)
            ConnectionAttemptStatus predicted = profile.transition()
            if (predicted != ConnectionAttemptStatus.FAILED)
                rv.add(d)
            else
                cannotTry.add(d)
        }
        
        log.fine("got ${rv.size()} from profiles, cannot try ${cannotTry.size()}")
        rv
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
    public synchronized void start(Supplier<Collection<Destination>> connected) {
        this.connSupplier = connected
        
        def db = [ url : "jdbc:h2:" + h2Home.getAbsolutePath(),
            user : "muwire",
            password : "",
            driver : "org.h2.Driver" ]
        sql = Sql.newInstance(db.url, db.user, db.password, db.driver)
        
        boolean success = sql.execute("CREATE TABLE IF NOT EXISTS HOST_ATTEMPTS(DESTINATION VARCHAR(1024), TSTAMP TIMESTAMP," +
                    "STATUS ENUM('SUCCESSFUL','REJECTED','FAILED'))")
        log.info("created table attempts $success")
        
        // TODO add primary key
        success = sql.execute("CREATE TABLE IF NOT EXISTS HOST_PROFILES(" +
            "DESTINATION VARCHAR(1024)," +
            "SS VARCHAR(16)," +
            "SR VARCHAR(16)," +
            "SF VARCHAR(16)," +
            "RS VARCHAR(16)," +
            "RR VARCHAR(16)," +
            "RF VARCHAR(16)," +
            "FS VARCHAR(16)," +
            "FR VARCHAR(16)," +
            "FF VARCHAR(16)" +
            ")")

        timer.schedule({load()} as TimerTask, 1)        
        
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
                def tstamp = new Date(timestamp)
                tstamp = SDF.format(tstamp)
                sql.execute("insert into HOST_ATTEMPTS VALUES ('${dest.toBase64()}', '$tstamp', 'SUCCESSFUL')")
            }
            hostsJson.renameTo(new File(home, "hosts.json.bak"))
            log.info("migrated hosts.json")
        }
        
        // 2. load each host from DB and rank
        log.info("loading hosts from db")
        sql.eachRow("select distinct DESTINATION from HOST_ATTEMPTS") { 
            Destination dest = new Destination(it.DESTINATION)
            if (uniqueHosts.add(dest)) {
                def fromDB = sql.firstRow("select from HOST_PROFILES where DESTINATION=${dest.toBase64()}")
                def profile = new HostMCProfile()
                if (fromDB != null)
                    profile = new HostMCProfile(fromDB)
                profiles.put(dest, profile)
                allHosts.add(dest)
            }
        }
        
        log.fine("loaded ${allHosts.size()} hosts from db")
        timer.schedule({verifyHosts()} as TimerTask, 60000, 60000)
        loaded = true
    }
    
    private synchronized void verifyHosts() {
        log.fine("starting verification")
        final long now = System.currentTimeMillis()
        def nowTstamp = new java.sql.Date(now)
        def hourAgo = new java.sql.Date(now - 60*60*1000)
        
        List<String> allHosts = new ArrayList<>()
        sql.eachRow("select distinct DESTINATION from HOST_ATTEMPTS") {
            allHosts.add(it.DESTINATION)
        }
        
        List<Destination> allOldDests = new ArrayList<>()
        allHosts.each { 
            if(sql.firstRow("select TSTAMP from HOST_ATTEMPTS where DESTINATION=$it and TSTAMP > $hourAgo") == null)
                allOldDests.add(new Destination(it))
        }
        
        Collection<Destination> connected = this.connSupplier.get()
        allOldDests.each { 
            if (connected.contains(it)) {
                log.fine("${it.toBase32()} is connected, updating db")
                sql.execute("insert into HOST_ATTEMPTS values ('${it.toBase64()}', '$nowTstamp', 'SUCCESSFUL')")
            } else {
                log.fine("${it.toBase32()} will get pinged")
                toVerify.add(it)
            }
        }
        log.fine("end verification")
    }
    
}