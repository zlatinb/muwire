package com.muwire.gui.wizard

import com.muwire.core.MuWireSettings

abstract class WizardStep {

    final String constraint
    final WizardDefaults defaults
    
    protected WizardStep(String constraint, WizardDefaults defaults) {
        this.constraint = constraint
        this.defaults = defaults
    }
    
    
    protected abstract void buildUI(FactoryBuilderSupport builder)
    
    /**
     * @return list of errors, null if validation is successful
     */
    protected abstract List<String> validate()
    
    protected abstract void apply(MuWireSettings muSettings, Properties i2pSettings)
}
