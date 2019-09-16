import griffon.core.GriffonApplication
import groovy.util.logging.Log
import net.i2p.util.SystemVersion

import org.codehaus.griffon.runtime.core.AbstractLifecycleHandler

import com.muwire.core.Core
import com.muwire.core.MuWireSettings
import com.muwire.gui.UISettings

import javax.annotation.Nonnull
import javax.inject.Inject
import javax.swing.JTable
import javax.swing.LookAndFeel
import javax.swing.UIManager

import static griffon.util.GriffonApplicationUtils.isMacOSX
import static groovy.swing.SwingBuilder.lookAndFeel

import java.awt.Font
import java.util.logging.Level

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
        if (guiPropsFile.exists()) {
            Properties props = new Properties()
            guiPropsFile.withInputStream { props.load(it) }
            uiSettings = new UISettings(props)

            log.info("settting user-specified lnf $uiSettings.lnf")
            try {
                lookAndFeel(uiSettings.lnf)
            } catch (Throwable bad) {
                log.log(Level.WARNING,"couldn't set desired look and feeel, switching to defaults", bad)
                uiSettings.lnf = lookAndFeel("system","gtk","metal").getID()
            }

            if (uiSettings.font != null) {
                log.info("setting user-specified font $uiSettings.font")
                Font font = new Font(uiSettings.font, Font.PLAIN, 12)
                def defaults = UIManager.getDefaults()
                defaults.put("Button.font", font)
                defaults.put("RadioButton.font", font)
                defaults.put("Label.font", font)
                defaults.put("CheckBox.font", font)
                defaults.put("Table.font", font)
                defaults.put("TableHeader.font", font)
                // TODO: add others
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

