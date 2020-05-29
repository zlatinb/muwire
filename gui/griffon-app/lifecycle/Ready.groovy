
import griffon.core.GriffonApplication
import griffon.core.env.Metadata
import groovy.util.logging.Log
import net.i2p.util.SystemVersion

import org.codehaus.griffon.runtime.core.AbstractLifecycleHandler

import com.muwire.core.Constants
import com.muwire.core.Core
import com.muwire.core.MuWireSettings
import com.muwire.core.UILoadedEvent
import com.muwire.core.files.FileSharedEvent
import com.muwire.core.util.DataUtil

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
            propsFile.withReader("UTF-8", {
                props.load(it)
            })
            if (!props.containsKey("nickname"))
                props.setProperty("nickname", selectNickname())
            props = new MuWireSettings(props)
            if (props.incompleteLocation == null)
                props.incompleteLocation = new File(home, "incompletes")
        } else {
            log.info("creating new properties")
            props = new MuWireSettings()
            boolean embeddedRouterAvailable = Boolean.parseBoolean(System.getProperties().getProperty("embeddedRouter"))
            
            def parent = application.windowManager.findWindow("event-list")
            Properties i2pProps = new Properties()
            
            def params = [:]
            params['parent'] = parent
            params['embeddedRouterAvailable'] = embeddedRouterAvailable
            params['muSettings'] = props
            params['i2pProps'] = i2pProps
            def finished = [:]
            params['finished'] = finished
            
            application.mvcGroupManager.createMVCGroup("wizard", params)
            
            if (!finished['applied']) {
                JOptionPane.showMessageDialog(parent, "MuWire will now exit")
                System.exit(0)
            }
            
            File i2pPropsFile = new File(home, "i2p.properties")
            i2pPropsFile.withPrintWriter { i2pProps.store(it, "") }     
            
            props.embeddedRouter = embeddedRouterAvailable
            props.updateType = System.getProperty("updateType","jar")
                   
            
            propsFile.withPrintWriter("UTF-8", {
                props.write(it)
            })
        }

        Core core
        try {
            core = new Core(props, home, metadata["application.version"])
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
        } catch (Exception bad) {
            log.log(Level.SEVERE,"couldn't initialize core",bad)
            JOptionPane.showMessageDialog(null, "Couldn't connect to I2P router.  Make sure I2P is running and restart MuWire",
                    "Can't connect to I2P router", JOptionPane.WARNING_MESSAGE)
            System.exit(0)
        }
    }
    
    private String selectNickname() {
        String nickname
        while (true) {
            nickname = JOptionPane.showInputDialog(null,
                    "Your nickname is displayed when you send search results so other MuWire users can choose to trust you",
                    "Please choose a nickname", JOptionPane.PLAIN_MESSAGE)
                    if (nickname == null) {
                        JOptionPane.showMessageDialog(null, "MuWire cannot start without a nickname and will now exit", JOptionPane.PLAIN_MESSAGE)
                        System.exit(0)
                    }
            if (nickname.trim().length() == 0) {
                JOptionPane.showMessageDialog(null, "Nickname cannot be empty", "Select another nickname",
                        JOptionPane.WARNING_MESSAGE)
                continue
            }
            if (!DataUtil.isValidName(nickname)) {
                JOptionPane.showMessageDialog(null, 
                    "Nickname cannot contain any of ${Constants.INVALID_NICKNAME_CHARS} and must be no longer than ${Constants.MAX_NICKNAME_LENGTH} characters.  Choose another.",
                        "Select another nickname", JOptionPane.WARNING_MESSAGE)
                continue
            }
            nickname = nickname.trim()
            break
        }
        nickname
    }
}

