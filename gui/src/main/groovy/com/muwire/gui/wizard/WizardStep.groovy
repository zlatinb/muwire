package com.muwire.gui.wizard

import com.muwire.core.MuWireSettings

abstract class WizardStep {

    final String constraint
    
    protected WizardStep(String constraint) {
        this.constraint = constraint
    }
    
    
    protected abstract void buildUI(FactoryBuilderSupport builder)
    
    /**
     * @return list of errors, null if validation is successful
     */
    protected abstract List<String> validate()
    
    protected abstract void apply(MuWireSettings muSettings, Properties i2pSettings)
}
