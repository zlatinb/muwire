
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
import com.muwire.gui.wizard.WizardDefaults

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
            props = new MuWireSettings(props)
            if (props.incompleteLocation == null)
                props.incompleteLocation = new File(home, "incompletes")
        } else {
            log.info("creating new properties")
            props = new MuWireSettings()
            boolean embeddedRouterAvailable = Boolean.parseBoolean(System.getProperties().getProperty("embeddedRouter"))
            
            def defaults
            if (System.getProperties().containsKey("wizard.defaults")) {
                File defaultsFile = new File(System.getProperty("wizard.defaults"))
                Properties defaultsProps = new Properties()
                defaultsFile.withInputStream { defaultsProps.load(it) }
                defaults = new WizardDefaults(defaultsProps)
            } else
                defaults = new WizardDefaults()
            
            def parent = application.windowManager.findWindow("event-list")
            Properties i2pProps = new Properties()
            
            def params = [:]
            params['parent'] = parent
            params['defaults'] = defaults
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
}

