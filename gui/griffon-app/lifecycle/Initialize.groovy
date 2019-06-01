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
class Initialize extends AbstractLifecycleHandler {
    @Inject
    Initialize(@Nonnull GriffonApplication application) {
        super(application)
    }

    @Override
    void execute() {
        if (isMacOSX()) {
            lookAndFeel('nimbus') // otherwise the file chooser doesn't open???
        } else {
            lookAndFeel('system', 'gtk')
        }
    }
}

