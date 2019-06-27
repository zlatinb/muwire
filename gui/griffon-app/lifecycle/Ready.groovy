
import griffon.core.GriffonApplication
import griffon.core.env.Metadata
import groovy.util.logging.Log
import net.i2p.util.SystemVersion

import org.codehaus.griffon.runtime.core.AbstractLifecycleHandler

import com.muwire.core.Core
import com.muwire.core.MuWireSettings
import com.muwire.core.UILoadedEvent
import com.muwire.core.files.FileSharedEvent

import javax.annotation.Nonnull
import javax.inject.Inject
import javax.swing.JFileChooser
import javax.swing.JOptionPane

import static griffon.util.GriffonApplicationUtils.isMacOSX
import static groovy.swing.SwingBuilder.lookAndFeel

import java.beans.PropertyChangeEvent
import java.util.logging.Level

@Log
class Ready extends AbstractLifecycleHandler {
    
    @Inject Metadata metadata
    
    @Inject
    Ready(@Nonnull GriffonApplication application) {
        super(application)
    }

    @Override
    void execute() {
        log.info "starting core services"
        
        def home = new File(application.getContext().getAsString("muwire-home"))
        def props = new Properties()
        def propsFile = new File(home, "MuWire.properties")
        if (propsFile.exists()) {
            log.info("loading existing props file")
            propsFile.withInputStream {
                props.load(it)
            }
            props = new MuWireSettings(props)
        } else {
            log.info("creating new properties")
            props = new MuWireSettings()
            props.embeddedRouter = Boolean.parseBoolean(System.getProperties().getProperty("embeddedRouter"))
            def nickname
            while (true) {
                nickname = JOptionPane.showInputDialog(null,
                        "Your nickname is displayed when you send search results so other MuWire users can choose to trust you",
                        "Please choose a nickname", JOptionPane.PLAIN_MESSAGE)
                if (nickname == null || nickname.trim().length() == 0) {
                    JOptionPane.showMessageDialog(null, "Nickname cannot be empty", "Select another nickname", 
                        JOptionPane.WARNING_MESSAGE)
                    continue
                }
                if (nickname.contains("@")) {
                    JOptionPane.showMessageDialog(null, "Nickname cannot contain @, choose another", 
                        "Select another nickname", JOptionPane.WARNING_MESSAGE)
                    continue
                }
                nickname = nickname.trim()
                break
            }
            props.setNickname(nickname)
            
            
            def portableDownloads = System.getProperty("portable.downloads")
            if (portableDownloads != null) {
                props.downloadLocation = new File(portableDownloads)
            } else {
                def chooser = new JFileChooser()
                chooser.setFileHidingEnabled(false)
                chooser.setDialogTitle("Select a directory where downloads will be saved")
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY)
                int rv = chooser.showOpenDialog(null)
                if (rv != JFileChooser.APPROVE_OPTION) {
                    JOptionPane.showMessageDialog(null, "MuWire will now exit")
                    System.exit(0)
                }
                props.downloadLocation = chooser.getSelectedFile()
            }
            
            propsFile.withOutputStream {
                props.write(it)
            }
        }
        
        Core core
        try {
            core = new Core(props, home, metadata["application.version"])
        } catch (Exception bad) {
            log.log(Level.SEVERE,"couldn't initialize core",bad)
            JOptionPane.showMessageDialog(null, "Couldn't connect to I2P router.  Make sure I2P is running and restart MuWire",
                "Can't connect to I2P router", JOptionPane.WARNING_MESSAGE)
            System.exit(0)
        }
        Runtime.getRuntime().addShutdownHook({
            core.shutdown() 
        })
        core.startServices()
        application.context.put("muwire-settings", props)
        application.context.put("core",core)
        application.getPropertyChangeListeners("core").each { 
            it.propertyChange(new PropertyChangeEvent(this, "core", null, core)) 
        }
        
        core.eventBus.publish(new UILoadedEvent())
    }
}

