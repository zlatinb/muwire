package com.muwire.cli

import com.muwire.core.util.DataUtil

import groovy.json.JsonSlurper
import net.i2p.data.Base64

class FileList {
    public static void main(String [] args) {
        if (args.length < 1) {
            println "pass files.json as argument"
            System.exit(1)
        }
        
        def slurper = new JsonSlurper()
        File filesJson = new File(args[0])
        filesJson.eachLine { 
            def json = slurper.parseText(it)
            String name = DataUtil.readi18nString(Base64.decode(json.file))
            println "$name,$json.length,$json.pieceSize,$json.infoHash"
        }
    }
}
