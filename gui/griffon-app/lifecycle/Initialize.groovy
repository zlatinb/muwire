import griffon.core.GriffonApplication
import groovy.swing.SwingBuilder
import groovy.util.logging.Log
import net.i2p.util.SystemVersion

import org.codehaus.griffon.runtime.core.AbstractLifecycleHandler

import com.muwire.core.Core
import com.muwire.core.MuWireSettings
import com.muwire.gui.Translator
import com.muwire.gui.UISettings

import javax.annotation.Nonnull
import javax.inject.Inject
import javax.swing.ImageIcon
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPopupMenu
import javax.swing.JTable
import javax.swing.LookAndFeel
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.swing.plaf.FontUIResource

import static griffon.util.GriffonApplicationUtils.isMacOSX
import static groovy.swing.SwingBuilder.lookAndFeel

import java.awt.BorderLayout
import java.awt.Font
import java.awt.MenuItem
import java.awt.PopupMenu
import java.awt.SystemTray
import java.awt.Toolkit
import java.awt.TrayIcon
import java.util.concurrent.CountDownLatch
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
        int rowHeight = 15
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

            if (uiSettings.font != null || uiSettings.autoFontSize || uiSettings.fontSize > 0 ) {

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
                rowHeight = fontSize + 3
                FontUIResource font = new FontUIResource(fontName, uiSettings.fontStyle, fontSize)
                
                def keys = lnf.getDefaults().keys()
                while(keys.hasMoreElements()) {
                    def key = keys.nextElement()
                    def value = lnf.getDefaults().get(key)
                    if (value instanceof FontUIResource) {
                        lnf.getDefaults().put(key, font)
                        UIManager.put(key, font)
                    }
                }
            }
        } else {
            Properties props = new Properties()
            uiSettings = new UISettings(props)
            log.info "will try default lnfs"
            
            LookAndFeel chosen = lookAndFeel('system', 'gtk', 'metal')
            uiSettings.lnf = chosen.getID()
            log.info("ended up applying $chosen.name")

            FontUIResource defaultFont = chosen.getDefaults().getFont("Label.font")
            uiSettings.font = defaultFont.getName()
            uiSettings.fontSize = defaultFont.getSize()
            uiSettings.fontStyle = defaultFont.getStyle()
            rowHeight = uiSettings.fontSize + 3
            
            uiSettings.locale = showLanguageDialog()
        }

        application.context.put("row-height", rowHeight)
        application.context.put("ui-settings", uiSettings)
        
        Translator.setLocale(uiSettings.locale);
        
        System.setProperty("apple.eawt.quitStrategy", "CLOSE_ALL_WINDOWS");
        
        if (SystemTray.isSupported()) {
            try {
                def tray = SystemTray.getSystemTray()
                def url = Initialize.class.getResource("/MuWire-16x16.png")
                def image = new ImageIcon(url, "tray icon").getImage()
                def popupMenu = new PopupMenu()
                def trayIcon = new TrayIcon(image, "MuWire", popupMenu)


                def exit = new MenuItem(Translator.trans("EXIT"))
                exit.addActionListener({
                    def mainFrame = application.getMvcGroupManager().findGroup("MainFrame")
                    if (mainFrame != null)
                        mainFrame.view.closeApplication()
                    else
                        application.shutdown()
                    tray.remove(trayIcon)
                })
                
                def showMW = {e ->
                    def mainFrame = application.getWindowManager().findWindow("main-frame")
                    if (mainFrame != null) {
                        Core core = application.getContext().get("core")
                        if (core != null)
                            mainFrame.setVisible(true)
                    }
                }

                def show = new MenuItem(Translator.trans("OPEN_MUWIRE"))
                show.addActionListener(showMW)
                popupMenu.add(show)
                popupMenu.add(exit)
                tray.add(trayIcon)
                
                
                trayIcon.addActionListener(showMW)
                application.getContext().put("tray-icon", trayIcon)
            } catch (Exception bad) {
                log.log(Level.WARNING,"couldn't set tray icon",bad)
            }
        }
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

    private String showLanguageDialog() {
        if (Translator.SUPPORTED_LOCALES.size() == 1)
            return Locale.US.toLanguageTag()
        def builder = new SwingBuilder()
        def languageComboBox
        def chooseButton
        def frame = builder.frame (visible : true, locationRelativeTo: null,
        defaultCloseOperation : JFrame.EXIT_ON_CLOSE,
        iconImage : builder.imageIcon("/MuWire-48x48.png").image) {
            borderLayout()
            panel(constraints : BorderLayout.NORTH) {
                label("Select Language")
            }
            
            languageComboBox = comboBox (editable: false, items : Translator.LOCALE_WRAPPERS, constraints : BorderLayout.CENTER)
            
            panel (constraints : BorderLayout.SOUTH) {
                chooseButton = button(text : "Choose")
            }
        }
        
        CountDownLatch latch = new CountDownLatch(1)
        chooseButton.addActionListener({
            frame.setVisible(false)
            latch.countDown()
        })
        SwingUtilities.invokeAndWait({
            frame.pack()
            frame.setVisible(true)
            frame.requestFocus()
        })
        latch.await()
        languageComboBox.getSelectedItem().locale.toLanguageTag()
    }
}

