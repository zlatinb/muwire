import griffon.core.GriffonApplication
import groovy.util.logging.Log

import org.codehaus.griffon.runtime.core.AbstractLifecycleHandler

import com.muwire.core.Core
import com.muwire.core.MuWireSettings

import javax.annotation.Nonnull
import javax.inject.Inject

import static griffon.util.GriffonApplicationUtils.isMacOSX
import static groovy.swing.SwingBuilder.lookAndFeel

@Log
class Ready extends AbstractLifecycleHandler {
    @Inject
    Ready(@Nonnull GriffonApplication application) {
        super(application)
    }

    @Override
    void execute() {
        log.info "starting core services"
        def home = System.getProperty("user.home") + File.separator + ".MuWire"
        home = new File(home)
        if (!home.exists()) {
            log.info("creating home dir")
            home.mkdir()
        }
        
        def props = new Properties()
        def propsFile = new File(home, "MuWire.properties")
        if (propsFile.exists()) {
            log.info("loading existing props file")
            propsFile.withInputStream {
                props.load(it)
            }
            props = new MuWireSettings(props)
        } else {
            log.info("creating default properties")
            props = new MuWireSettings()
            propsFile.withOutputStream {
                props.write(it)
            }
        }
        
        Core core = new Core(props, home)
        core.startServices()
        application.context.put("core",core)
    }
}

