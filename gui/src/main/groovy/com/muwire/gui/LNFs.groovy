package com.muwire.gui

import javax.swing.UIManager

/**
 * looks and feels
 */
class LNFs {
    static final Map<String, String> nameToClass = new HashMap<>()
    static final Map<String, String> classToName = new HashMap<>()
    static final Set<String> SYSTEM_ALIASES = new HashSet<>()
    
    static final String[] availableLNFs
    
    private static register(String name, String clazz) {
        try {
            Class cls = Class.forName(clazz)
            nameToClass.put(name, cls.getCanonicalName())
            classToName.put(cls.getCanonicalName(), name)
        } catch (Exception notThere) {} // just not available
    }
    
    private static String camel(String s) {
        s.substring(0,1).toUpperCase() + s.substring(1)
    }
    
    private static String deriveJTattooClass(String shortName) {
        String lnfClass = camel(shortName)
        "com.jtattoo.plaf.${shortName}.${lnfClass}LookAndFeel"
    }
    
    static {
        SYSTEM_ALIASES.add("GTK") // Linux
        SYSTEM_ALIASES.add("Aqua") // Mac
        SYSTEM_ALIASES.add("Windows") // Windows
        
        register("System", UIManager.getSystemLookAndFeelClassName())
        register("Metal","javax.swing.plaf.metal.MetalLookAndFeel")
        register("Motif", "com.sun.java.swing.plaf.motif.MotifLookAndFeel")
        register("GTK", "com.sun.java.swing.plaf.gtk.GTKLookAndFeel")
        register("Windows", "com.sun.java.swing.plaf.windows.WindowsLookAndFeel")
        register("Aqua", "com.apple.laf.AquaLookAndFeel")
        register("Nimbus", "javax.swing.plaf.nimbus.NimbusLookAndFeel")
        register("Darcula","com.bulenkov.darcula.DarculaLaf")
        ["acryl",
                "aero",
                "aluminium",
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
        Arrays.sort(availableLNFs)
    }
    
    public static String getLNFClassName(String alias) {
        if (alias.toLowerCase() == "system")
            return UIManager.getSystemLookAndFeelClassName()
        nameToClass[alias]
    }
}
