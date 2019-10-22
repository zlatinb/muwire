import griffon.core.GriffonApplication
import groovy.util.logging.Log
import net.i2p.util.SystemVersion

import org.codehaus.griffon.runtime.core.AbstractLifecycleHandler

import com.muwire.core.Core
import com.muwire.core.MuWireSettings
import com.muwire.gui.UISettings

import javax.annotation.Nonnull
import javax.inject.Inject
import javax.swing.ImageIcon
import javax.swing.JLabel
import javax.swing.JPopupMenu
import javax.swing.JTable
import javax.swing.LookAndFeel
import javax.swing.UIManager
import javax.swing.plaf.FontUIResource

import static griffon.util.GriffonApplicationUtils.isMacOSX
import static groovy.swing.SwingBuilder.lookAndFeel

import java.awt.Font
import java.awt.MenuItem
import java.awt.PopupMenu
import java.awt.SystemTray
import java.awt.Toolkit
import java.awt.TrayIcon
import java.util.logging.Level
import java.util.logging.LogManager

@Log
class Initialize extends AbstractLifecycleHandler {
    @Inject
    Initialize(@Nonnull GriffonApplication application) {
        super(application)
    }

    @Override
    void execute() {
        
        if (System.getProperty("java.util.logging.config.file") == null) {
            log.info("No config file specified, so turning off most logging")
            def names = LogManager.getLogManager().getLoggerNames()
            while(names.hasMoreElements()) {
                def name = names.nextElement()
                LogManager.getLogManager().getLogger(name).setLevel(Level.SEVERE)
            }
        }
        
        if (SystemTray.isSupported()) {
            try {
                def tray = SystemTray.getSystemTray()
                def url = Initialize.class.getResource("/MuWire-32x32.png")
                def image = new ImageIcon(url, "tray icon").getImage()
                def popupMenu = new PopupMenu()
                def trayIcon = new TrayIcon(image, "MuWire", popupMenu)


                def exit = new MenuItem("Exit")
                exit.addActionListener({
                    Core core = application.getContext().get("core")
                    if (core != null)
                        core.shutdown()
                    tray.remove(trayIcon)
                    System.exit(0)
                })

                popupMenu.add(exit)
                tray.add(trayIcon)
                
                
                trayIcon.addActionListener({ e ->
                    def mainFrame = application.getWindowManager().findWindow("main-frame")
                    if (mainFrame != null)
                        mainFrame.setVisible(true)
                })
                application.getContext().put("tray-icon", true)
            } catch (Exception bad) {
                log.log(Level.WARNING,"couldn't set tray icon",bad)
            }
        }
        
        log.info "Loading home dir"
        def portableHome = System.getProperty("portable.home")
        def home = portableHome == null ?
            selectHome() :
            portableHome

        home = new File(home)
        if (!home.exists()) {
            log.info("creating home dir $home")
            home.mkdirs()
        }

        application.context.put("muwire-home", home.getAbsolutePath())

        System.getProperties().setProperty("awt.useSystemAAFontSettings", "gasp")

        def guiPropsFile = new File(home, "gui.properties")
        UISettings uiSettings
        if (guiPropsFile.exists()) {
            Properties props = new Properties()
            guiPropsFile.withInputStream { props.load(it) }
            uiSettings = new UISettings(props)

            def lnf
            log.info("settting user-specified lnf $uiSettings.lnf")
            try {
                lnf = lookAndFeel(uiSettings.lnf)
            } catch (Throwable bad) {
                log.log(Level.WARNING,"couldn't set desired look and feel, switching to defaults", bad)
                lnf = lookAndFeel("system","gtk","metal")
                uiSettings.lnf = lnf.getID()
            }

            if (uiSettings.font != null || uiSettings.autoFontSize || uiSettings.fontSize > 0) {

                FontUIResource defaultFont = lnf.getDefaults().getFont("Label.font")
                
                String fontName
                if (uiSettings.font != null)
                    fontName = uiSettings.font
                else
                    fontName = defaultFont.getName()
                
                int fontSize = defaultFont.getSize()
                if (uiSettings.autoFontSize) {
                    int resolution = Toolkit.getDefaultToolkit().getScreenResolution()
                    fontSize = resolution / 9;
                } else {
                    fontSize = uiSettings.fontSize
                }
                
                FontUIResource font = new FontUIResource(fontName, Font.PLAIN, fontSize)
                
                def keys = lnf.getDefaults().keys()
                while(keys.hasMoreElements()) {
                    def key = keys.nextElement()
                    def value = lnf.getDefaults().get(key)
                    if (value instanceof FontUIResource)
                        lnf.getDefaults().put(key, font)
                }
            }
        } else {
            Properties props = new Properties()
            uiSettings = new UISettings(props)
            log.info "will try default lnfs"
            if (isMacOSX()) {
                if (SystemVersion.isJava9()) {
                    uiSettings.lnf = "metal"
                    lookAndFeel("metal")
                } else {
                    uiSettings.lnf = "nimbus"
                    lookAndFeel('nimbus') // otherwise the file chooser doesn't open???
                }
            } else {
                LookAndFeel chosen = lookAndFeel('system', 'gtk')
                if (chosen == null)
                    chosen = lookAndFeel('metal')
                uiSettings.lnf = chosen.getID()
                log.info("ended up applying $chosen.name")
            }
        }

        application.context.put("ui-settings", uiSettings)
    }

    private static String selectHome() {
        def home = new File(System.properties["user.home"])
        def defaultHome = new File(home, ".MuWire")
        if (defaultHome.exists())
            return defaultHome.getAbsolutePath()
        if (SystemVersion.isMac()) {
            def library = new File(home, "Library")
            def appSupport = new File(library, "Application Support")
            def muwire = new File(appSupport,"MuWire")
            return muwire.getAbsolutePath()
        }
        if (SystemVersion.isWindows()) {
            def appData = new File(home,"AppData")
            def roaming = new File(appData, "Roaming")
            def muwire = new File(roaming, "MuWire")
            return muwire.getAbsolutePath()
        }
        defaultHome.getAbsolutePath()
    }
}

