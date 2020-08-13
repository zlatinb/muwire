package com.muwire.tracker

class SetupWizard {
    
    private final File home
    
    SetupWizard(File home) {
        this.home = home
    }
    
    Properties performSetup() {
        println "**** Welcome to mwtrackerd setup wizard *****"
        println "This wizard ask you some questions and configure the settings for the MuWire tracker daemon."
        println "The settings will be saved in ${home.getAbsolutePath()} where you can edit them manually if you wish."
        println "You can re-run this wizard by launching mwtrackerd with the \"setup\" argument."
        println "*****************"
        
        Scanner scanner = new Scanner(System.in)

        Properties rv = new Properties()        
        
        // nickname
        while(true) {
            println "Please select a nickname for your tracker"
            String nick = scanner.nextLine()
            if (nick.trim().length() == 0) {
                println "nickname cannot be empty"
                continue
            }
            rv['nickname'] = nick
            break
        }
        
        
        // i2cp host and port
        println "Enter the address of an I2P or I2Pd router to connect to.  (default is 127.0.0.1)"
        String i2cpHost = scanner.nextLine()
        if (i2cpHost.trim().length() == 0)
            i2cpHost = "127.0.0.1"
        rv['i2cp.tcp.host'] = i2cpHost
        
        println "Enter the port of the I2CP interface of the I2P[d] router (default is 7654)"
        String i2cpPort = scanner.nextLine()
        if (i2cpPort.trim().length() == 0)
            i2cpPort = "7654"
        rv['i2cp.tcp.port'] = i2cpPort
        
        // json-rpc interface
        println "Enter the address to which to bind the JSON-RPC interface of the tracker."
        println "Default is 127.0.0.1.  If you want to allow JSON-RPC connections from other hosts you can enter 0.0.0.0"
        String jsonRpcIface = scanner.nextLine()
        if (jsonRpcIface.trim().length() == 0)
            jsonRpcIface = "127.0.0.1"
        rv['jsonrpc.iface'] = jsonRpcIface
        
        println "Enter the port on which the JSON-RPC interface should listen.  (default is 12345)"
        String jsonRpcPort = scanner.nextLine()
        if (jsonRpcPort.trim().length() == 0)
            jsonRpcPort = "12345"
        rv['jsonrpc.port'] = jsonRpcPort
        
        // that's all
        println "*****************"
        println "That's all the setup that's required to get the tracker up and running."
        println "The tracker has many other settings which can be changed in the config files."
        println "Refer to the documentation for their description."
        println "*****************"
        
        rv
    }
}
