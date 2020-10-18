package com.muwire.core.hostcache

import com.muwire.core.MuWireSettings
import com.muwire.core.connection.ConnectionAttemptStatus
import com.muwire.core.trust.TrustService

import net.i2p.data.Destination

import groovy.sql.Sql
import groovy.util.logging.Log
@Log
class H2HostCache extends HostCache {
    
    private Sql sql
    private File h2Home
    private Set<Destination> hosts = new HashSet<>()
    
    public H2HostCache(File home, TrustService trustService, MuWireSettings settings, Destination myself) {
        super(trustService, settings, myself)
        h2Home = new File(home, "h2")
        if (!h2Home.exists())
            h2Home.mkdir()
    }
    @Override
    protected synchronized void hostDiscovered(Destination d, boolean fromHostcache) {
        if (fromHostcache) 
            sql.execute("delete from HOST_ATTEMPTS where DESTINATION='${d.toBase64()}';")
        hosts.add(d)
    }
    
    @Override
    protected synchronized void onConnection(Destination d, ConnectionAttemptStatus status) {
        // record into db
        Date timestamp = new Date()
        sql.execute("insert into HOST_ATTEMPTS values ('${d.toBase64()}', '$timestamp', '${status.name()}');")
    }
    @Override
    public synchronized List<Destination> getHosts(int n) {
        // TODO Auto-generated method stub
        return Collections.emptyList();
    }
    @Override
    public synchronized List<Destination> getGoodHosts(int n) {
        // TODO Auto-generated method stub
        return Collections.emptyList();
    }
    @Override
    public synchronized int countFailingHosts() {
        // TODO Auto-generated method stub
        return 0;
    }
    @Override
    public synchronized int countHopelessHosts() {
        // TODO Auto-generated method stub
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
        loaded = true
        
    }
    @Override
    public synchronized void stop() {
        sql.close()
    }
    @Override
    public void load() {
    }
}