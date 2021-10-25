package com.muwire.gui

/**
 * looks and feels
 */
class LNFs {
    static final Map<String, String> nameToClass = new HashMap<>()
    static final Map<String, String> classToName = new HashMap<>()
    
    static final String[] availableLNFs
    
    private static register(String name, String clazz) {
        nameToClass.put(name, clazz)
        classToName.put(clazz, name)
    }
    
    private static String camel(String s) {
        s.substring(0,1).toUpperCase() + s.substring(1)
    }
    
    private static String deriveJTattooClass(String shortName) {
        String lnfClass = camel(shortName)
        "com.jtattoo.plaf.${shortName}.${lnfClass}LookAndFeel"
    }
    
    static {
        register("System", "system")
        register("Metal","metal")
        register("Darcula","com.bulenkov.darcula.DarculaLaf")
        ["acryl",
                "aero",
                "aluminum",
                "bernstein",
                "fast",
                "graphite",
                "luna",
                "mint",
                "noire",
                "smart",
                "texture"].each {register(camel(it), deriveJTattooClass(it))}
        register("HiFi","com.jtattoo.plaf.hifi.HiFiLookAndFeel")
        register("McWin","com.jtattoo.plaf.mcwin.McWinLookAndFeel")
        availableLNFs = nameToClass.keySet().toArray(new String[0])
    }
    
    
}
