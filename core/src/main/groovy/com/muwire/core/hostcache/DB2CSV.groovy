package com.muwire.core.hostcache

import groovy.sql.Sql
import net.i2p.data.Destination

class DB2CSV {

    static void main(args) {
        File h2 = new File(System.getProperty("user.home"))
        h2 = new File(h2, ".MuWire")
        h2 = new File(h2, "h2")
        
        def db = [ url : "jdbc:h2:" + h2.getAbsolutePath(),
            user : "muwire",
            password : "",
            driver : "org.h2.Driver" ]
        def sql = Sql.newInstance(db.url, db.user, db.password, db.driver)
        
        sql.eachRow("select * from HOST_ATTEMPTS") { 
            Destination dest = new Destination(it.DESTINATION)
            println "${dest.toBase32()},$it.TSTAMP,$it.STATUS"
        }
        
        sql.close()
    }
}
