package com.muwire.gui.wizard

class WizardDefaults {
    
    String downloadLocation
    String incompleteLocation
    String i2cpHost
    int i2cpPort
    int i2npTcpPort
    int i2npUdpPort
    int inBw, outBw
    int tunnelLength, tunnelQuantity

    WizardDefaults() {
        this(new Properties())
    }
    
    WizardDefaults(Properties props) {
        downloadLocation = props.getProperty("downloadLocation", getDefaultPath("MuWire Downloads"))
        incompleteLocation = props.getProperty("incompleteLocation", getDefaultPath("MuWire Incompletes"))
        i2cpHost = props.getProperty("i2cpHost","127.0.0.1")
        i2cpPort = Integer.parseInt(props.getProperty("i2cpPort","7654"))

        Random r = new Random()
        int randomPort = 9151 + r.nextInt(1 + 30777 - 9151)  // this range matches what the i2p router would choose
        
        i2npTcpPort = Integer.parseInt(props.getProperty("i2npTcpPort", String.valueOf(randomPort)))
        i2npUdpPort = Integer.parseInt(props.getProperty("i2npUdpPort", String.valueOf(randomPort)))
        
        inBw = Integer.parseInt(props.getProperty("inBw","1024"))
        outBw = Integer.parseInt(props.getProperty("outBw","512"))
        
        tunnelLength = Integer.parseInt(props.getProperty("tunnelLength","3"))
        tunnelQuantity = Integer.parseInt(props.getProperty("tunnelQuantity","4"))
    }
    
    private static String getDefaultPath(String pathName) {
        File f = new File(System.getProperty("user.home"), pathName)
        f.getAbsolutePath()
    }
}
