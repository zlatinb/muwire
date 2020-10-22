package com.muwire.core.hostcache

import groovy.sql.Sql
import net.i2p.data.Destination

class DB2CSV {

    static void main(args) {
        if (args.length != 1) {
            System.err.println("pass table name as parameter")
            System.exit(1)
        }
        File h2 = new File(System.getProperty("user.home"))
        h2 = new File(h2, ".MuWire")
        h2 = new File(h2, "h2")
        
        def db = [ url : "jdbc:h2:" + h2.getAbsolutePath(),
            user : "muwire",
            password : "",
            driver : "org.h2.Driver" ]
        def sql = Sql.newInstance(db.url, db.user, db.password, db.driver)
        
        switch(args[0]) {
            case "HOST_ATTEMPTS" :
                sql.eachRow("select * from HOST_ATTEMPTS") {
                    Destination dest = new Destination(it.DESTINATION)
                    println "${dest.toBase32()},$it.TSTAMP,$it.STATUS"
                }
                break
            case "HOST_PROFILES" :
                sql.eachRow("select * from HOST_PROFILES") {
                    Destination dest = new Destination(it.DESTINATION)
                    println "${dest.toBase32()},$it.SS,$it.SR,$it.SF,$it.RS,$it.RR,$it.RF,$it.FS,$it.FR,$it.FF"
                }
                break
        }
        sql.close()
    }
}
