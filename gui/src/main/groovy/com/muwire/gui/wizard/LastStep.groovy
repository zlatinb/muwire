package com.muwire.gui.wizard

import com.muwire.core.MuWireSettings

class LastStep extends WizardStep {
    
    private final boolean embeddedRouterAvailable

    public LastStep(boolean embeddedRouterAvailable) {
        super("last")
        this.embeddedRouterAvailable = embeddedRouterAvailable
    }

    @Override
    protected void buildUI(FactoryBuilderSupport builder) {
        builder.panel(constraints: getConstraint()) {
            label("The wizard is complete.  Press \"Finish\" to launch MuWire.")
            if (embeddedRouterAvailable)
                label("MuWire will launch an embedded I2P router.  This can take a few minutes.")
        }
    }

    @Override
    protected List<String> validate() {
        return null
    }

    @Override
    protected void apply(MuWireSettings muSettings, Properties i2pSettings) {
    }
}
