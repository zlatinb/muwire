
import javax.annotation.Nonnull
import javax.inject.Inject

import org.codehaus.griffon.runtime.core.AbstractLifecycleHandler

import com.muwire.core.Core

import griffon.core.GriffonApplication
import groovy.util.logging.Log

@Log
class Shutdown extends AbstractLifecycleHandler {
    @Inject
    Shutdown(@Nonnull GriffonApplication application) {
        super(application)
    }

    @Override
    void execute() {
        log.info("shutting down")
        Core core = application.context.get("core")
        core.shutdown()
    }
}
