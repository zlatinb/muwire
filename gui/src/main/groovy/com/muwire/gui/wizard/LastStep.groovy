package com.muwire.gui.wizard

import com.muwire.core.MuWireSettings

class LastStep extends WizardStep {
    
    private final boolean embeddedRouterAvailable

    public LastStep(boolean embeddedRouterAvailable) {
        super("last", null)
        this.embeddedRouterAvailable = embeddedRouterAvailable
    }

    @Override
    protected void buildUI(FactoryBuilderSupport builder) {
        builder.panel(constraints: getConstraint()) {
            gridBagLayout()
            label(text: "The wizard is complete.  Press \"Finish\" to launch MuWire.", constraints : gbc(gridx: 0, gridy: 0))
            if (embeddedRouterAvailable)
                label(text : "MuWire will launch an embedded I2P router.  This can take a few minutes.", constraints: gbc(gridx:0, gridy:1))
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
