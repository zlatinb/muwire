package com.muwire.gui.wizard

import com.muwire.core.MuWireSettings
import static com.muwire.gui.Translator.trans

class LastStep extends WizardStep {
    
    private final boolean embeddedRouterAvailable

    public LastStep(boolean embeddedRouterAvailable) {
        super("last", null)
        this.embeddedRouterAvailable = embeddedRouterAvailable
    }

    @Override
    protected void buildUI(FactoryBuilderSupport builder, def nextAction) {
        builder.panel(constraints: getConstraint()) {
            gridBagLayout()
            label(text: trans("WIZARD_COMPLETE"), constraints : gbc(gridx: 0, gridy: 0))
            if (embeddedRouterAvailable)
                label(text : trans("LAUNCH_EMBEDDED_ROUTER"), constraints: gbc(gridx:0, gridy:1))
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
