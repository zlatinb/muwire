package com.muwire.core.hostcache

import java.util.function.Predicate
import java.util.function.Supplier
import static java.lang.Double.isNaN

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
            sql.execute("delete from HOST_ATTEMPTS where DESTINATION=${d.toBase64()}")
            profiles.put(d, new HostMCProfile())
        }
        if (uniqueHosts.add(d)) {
            allHosts.add(d)
            profiles.put(d, new HostMCProfile())
            
            log.fine("learned about ${d.toBase32()} from hostcache $fromHostcache")
        }
    }
    
    @Override
    protected synchronized void onConnection(Destination d, ConnectionAttemptStatus status) {
        
        log.fine("onConnection ${d.toBase32()} status $status")
        if (uniqueHosts.add(d)) {
            allHosts.add(d)
            profiles.put(d, new HostMCProfile())
        } 
        if (status == ConnectionAttemptStatus.SUCCESSFUL) 
            profiles.get(d).successfulAttempt = true
            
        // record into db
        def timestamp = new Date(System.currentTimeMillis())
        timestamp = SDF.format(timestamp)
        sql.execute("insert into HOST_ATTEMPTS values ('${d.toBase64()}', '$timestamp', '${status.name()}');")
        
        def count = sql.firstRow("select count(*) as COUNT from HOST_ATTEMPTS where DESTINATION=${d.toBase64()}")
        if (count.COUNT < settings.minHostProfileHistory) {
            log.fine("not enough history for Markov") 
            profiles.put(d, new HostMCProfile(status))
            return
        }
        
        log.fine("recomputing Markov for ${d.toBase32()} from history items $count.COUNT")
        
        int ss = 0
        int sr = 0
        int sf = 0
        int rs = 0
        int rr = 0
        int rf = 0
        int fs = 0
        int fr = 0
        int ff = 0
        ConnectionAttemptStatus currentStatus = null
        
        sql.eachRow("select STATUS from HOST_ATTEMPTS where DESTINATION=${d.toBase64()} ORDER BY TSTAMP") {  
            def recordedStatus = ConnectionAttemptStatus.valueOf(it.STATUS)
            if (currentStatus == null) {
                currentStatus = recordedStatus
                return
            }
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
        
        int countS = ss + sr + sf
        int countR = rs + rr + rf
        int countF = fs + fr + ff
        
        double ssd = HostMCProfile.DEFAULT_SS
        double srd = HostMCProfile.DEFAULT_SR
        double sfd = HostMCProfile.DEFAULT_SF
        if (countS > 0) {
            ssd = ss * 1.0d / countS
            srd = sr * 1.0d / countS
            sfd = sf * 1.0d / countS
        }
        if (isNaN(ssd) || isNaN(srd) || isNaN(sfd)) {
            ssd = HostMCProfile.DEFAULT_SS
            srd = HostMCProfile.DEFAULT_RR
            sfd = HostMCProfile.DEFAULT_SF
        }
        
        double rsd = HostMCProfile.DEFAULT_RS
        double rrd = HostMCProfile.DEFAULT_RR
        double rfd = HostMCProfile.DEFAULT_RF
        if (countR > 0) {
            rsd = rs * 1.0d / countR
            rrd = rr * 1.0d / countR
            rfd = rf * 1.0d / countR
        }
        if (isNaN(rsd) || isNaN(rrd) || isNaN(rfd)) {
            rsd = HostMCProfile.DEFAULT_RS
            rrd = HostMCProfile.DEFAULT_RR
            rfd = HostMCProfile.DEFAULT_RF
        }
        
        double fsd = HostMCProfile.DEFAULT_FS
        double frd = HostMCProfile.DEFAULT_FR
        double ffd = HostMCProfile.DEFAULT_FF
        if (countF > 0) {
            fsd = fs * 1.0d / countF
            frd = fr * 1.0d / countF
            ffd = ff * 1.0d / countF
        }
        if (isNaN(fsd) || isNaN(frd) || isNaN(ffd)) {
            fsd = HostMCProfile.DEFAULT_FS
            frd = HostMCProfile.DEFAULT_FR
            ffd = HostMCProfile.DEFAULT_FF
        }
        
        sql.execute("delete from HOST_PROFILES where DESTINATION=${d.toBase64()}")
        sql.execute("insert into HOST_PROFILES values (" +
            "'${d.toBase64()}'," +
            "'${String.format(Locale.US, "%.6f",ssd)}'," +
            "'${String.format(Locale.US, "%.6f",srd)}'," +
            "'${String.format(Locale.US, "%.6f",sfd)}'," +
            "'${String.format(Locale.US, "%.6f",rsd)}'," +
            "'${String.format(Locale.US, "%.6f",rrd)}'," +
            "'${String.format(Locale.US, "%.6f",rfd)}'," +
            "'${String.format(Locale.US, "%.6f",fsd)}'," +
            "'${String.format(Locale.US, "%.6f",frd)}'," +
            "'${String.format(Locale.US, "%.6f",ffd)}'" +
            ")")
        def newProfile = sql.firstRow("select * from HOST_PROFILES where DESTINATION=${d.toBase64()}")
        profiles.put(d, new HostMCProfile(newProfile, status))
        log.fine("profile updated ${d.toBase32()} ${profiles.get(d)}")       

        sql.execute("delete from HOST_ATTEMPTS where DESTINATION=${d.toBase64()} and TSTAMP not in "+
            "(select TSTAMP from HOST_ATTEMPTS where DESTINATION=${d.toBase64()} order by TSTAMP desc limit $settings.hostProfileHistory)") 
        log.fine("deleted $sql.updateCount old attempts")
    }
    
    @Override
    public synchronized List<Destination> getHosts(int n, Predicate<Destination> filter) {
        if (allHosts.isEmpty())
            return Collections.emptyList()
        
        List<Destination> rv = new ArrayList<>()
        Iterator<Destination> verifyIter = toVerify.iterator()
        while((rv.size() < n) && (verifyIter.hasNext())) {
            def d = verifyIter.next()
            if (filter.test(d))
                rv.add(d)
            verifyIter.remove()
        }
        
        log.fine("got ${rv.size()} from toVerify list, which is now ${toVerify.size()}")
        
        if (rv.size() == n)
            return rv

        List<Destination> canTry = new ArrayList<>(allHosts)
        canTry.retainAll { !profiles.get(it).isHopeless() && filter.test(it)}
        Set<Destination> wouldFail = new HashSet<>()             
        while(rv.size() < n && wouldFail.size() < canTry.size()) {
            Destination d = canTry.get((int)(Math.random() * canTry.size()))
            if (wouldFail.contains(d))
                continue
                
            HostMCProfile profile = profiles.get(d)
            ConnectionAttemptStatus current = profile.state
            ConnectionAttemptStatus predicted = profile.nextState()
            log.fine("predicted $current -> $predicted for ${d.toBase32()} profile $profile")
            if (predicted != ConnectionAttemptStatus.FAILED)
                rv.add(d)
            else
                wouldFail.add(d)
        }
        
        log.fine("got ${rv.size()} from profiles, can try ${canTry.size()} would fail ${wouldFail.size()} total ${allHosts.size()}")
        rv
    }
    
    @Override
    public synchronized List<Destination> getGoodHosts(int n) {
        List<Destination> rv = new ArrayList<>(allHosts)
        rv.retainAll { profiles.get(it).shouldAdvertise() }
        if (rv.size() <= n)
            return rv
        Collections.shuffle(rv)
        rv[0..n-1]  
    }
    
    @Override
    public synchronized int countFailingHosts() {
        return 0;
    }
    
    @Override
    public synchronized int countHopelessHosts() {
        allHosts.count { profiles.get(it).isHopeless() }
    }
    
    public synchronized int countAllHosts() {
        allHosts.size()
    }
    
    private synchronized void initSQL() {
        def db = [ url : "jdbc:h2:" + h2Home.getAbsolutePath(),
            user : "muwire",
            password : "",
            driver : "org.h2.Driver" ]
        sql = Sql.newInstance(db.url, db.user, db.password, db.driver)
    }
    
    private synchronized void recycleSQL() {
        sql?.close()
        initSQL()
        System.gc()
    }
    
    @Override
    public synchronized void start(Supplier<Collection<Destination>> connected) {
        this.connSupplier = connected
        
        initSQL()   
        boolean success = sql.execute("CREATE TABLE IF NOT EXISTS HOST_ATTEMPTS(DESTINATION VARCHAR(1024), TSTAMP TIMESTAMP," +
                    "STATUS ENUM('SUCCESSFUL','REJECTED','FAILED'))")
        log.info("created table attempts $success")
        
        // TODO add primary key
        success &= sql.execute("CREATE TABLE IF NOT EXISTS HOST_PROFILES(" +
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

        success &= sql.execute("CREATE TABLE IF NOT EXISTS CONNECTION_COUNT(TSTAMP TIMESTAMP, COUNT INT)")
        timer.schedule({load()} as TimerTask, 1)        
        
    }
    @Override
    public synchronized void stop() {
        timer.cancel()
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
                def fromDB = sql.firstRow("select * from HOST_PROFILES where DESTINATION=${dest.toBase64()}")
                def profile = new HostMCProfile()
                if (fromDB != null) {
                    def lastObservation = sql.firstRow("select STATUS from HOST_ATTEMPTS where DESTINATION=${dest.toBase64()} order by TSTAMP desc limit 1")
                    if (lastObservation != null) 
                        profile = new HostMCProfile(fromDB, ConnectionAttemptStatus.valueOf(lastObservation.STATUS))
                }
                profiles.put(dest, profile)
                allHosts.add(dest)
                log.fine("Loaded profile for ${dest.toBase32()} $profile")
            }
        }
        
        log.fine("loaded ${allHosts.size()} hosts from db")
        timer.schedule({
            recycleSQL()
            purgeHopeless()
            verifyHosts()
            recordConnectionCount()
        } as TimerTask, 60000, 60000)
        loaded = true
    }
    
    private synchronized void verifyHosts() {
        log.fine("starting verification")
        final long now = System.currentTimeMillis()
        def nowTstamp = SDF.format(new Date(now))
        def hourAgo = SDF.format(new Date(now - 60*60*1000))
        
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
    
    private synchronized void purgeHopeless() {
        log.fine("purging hopeless hosts from total ${allHosts.size()}")
        
        final long now = System.currentTimeMillis()
        List<Destination> candidates = new ArrayList<>()
        for (Destination d : allHosts) {
            if (profiles.get(d).isHopeless()) {
                log.fine("considering hopeless host ${d.toBase32()}")
                def row = sql.firstRow("select TSTAMP from HOST_ATTEMPTS where DESTINATION=${d.toBase64()} and STATUS='SUCCESSFUL' order by TSTAMP DESC LIMIT 1")
                if (row == null) {
                    log.fine("no successful attempts")
                    candidates.add(d)
                } else {
                    if (now - row.TSTAMP.getTime() > settings.hostHopelessPurgeInterval * 60 * 1000) {
                        log.fine("last successful attempt was at $row.TSTAMP , purging")
                        candidates.add(d)
                    }
                }
            }
        }
        
        log.fine("will purge ${candidates.size()} hopeless hosts")
        for (Destination hopeless : candidates) {
            allHosts.remove(hopeless)
            uniqueHosts.remove(hopeless)
            sql.execute("delete from HOST_ATTEMPTS where DESTINATION=${hopeless.toBase64()}")
            sql.execute("delete from HOST_PROFILES where DESTINATION=${hopeless.toBase64()}")
        }
        log.fine("purged hopeless, remaining ${allHosts.size()}")
        
    }
    
    private synchronized void recordConnectionCount() {
        int count = connSupplier.get().size()
        def tstamp = SDF.format(new Date())
        sql.execute("insert into CONNECTION_COUNT values('$tstamp','$count')")
        sql.execute("delete from CONNECTION_COUNT where TSTAMP not in (select TSTAMP from CONNECTION_COUNT order by TSTAMP desc limit ${settings.connectionHistory})")
    }
    
}