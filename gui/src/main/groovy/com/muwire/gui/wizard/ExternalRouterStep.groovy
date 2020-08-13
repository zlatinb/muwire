package com.muwire.gui.wizard

import java.awt.GridBagConstraints

import javax.swing.border.TitledBorder

import com.muwire.core.MuWireSettings

class ExternalRouterStep extends WizardStep {

    def addressField
    def portField
    
    public ExternalRouterStep(WizardDefaults defaults) {
        super("router", defaults)
    }

    @Override
    protected void buildUI(FactoryBuilderSupport builder) {
        builder.panel(constraints : getConstraint()) {
            gridBagLayout()
            panel(border : titledBorder(title : "External Router I2CP Settings", border : etchedBorder(), titlePosition : TitledBorder.TOP,
                constraints : gbc(gridx: 0, gridy : 0, fill : GridBagConstraints.HORIZONTAL, weightx: 100))) {
                gridBagLayout()
            
                label(text : "Host", constraints : gbc(gridx: 0, gridy : 0, anchor : GridBagConstraints.LINE_START, weightx : 100))
                addressField = textField(text : defaults.i2cpHost, constraints : gbc(gridx:1, gridy:0, anchor: GridBagConstraints.LINE_END))
                
                label(text : "Port", constraints : gbc(gridx: 0, gridy : 1, anchor : GridBagConstraints.LINE_START, weightx : 100))
                portField = textField(text : String.valueOf(defaults.i2cpPort), constraints : gbc(gridx:1, gridy:1, anchor: GridBagConstraints.LINE_END))
            }
            panel(constraints : gbc(gridx:0, gridy:1, weighty: 100))
        }
    }

    @Override
    protected List<String> validate() {
        def rv = []
        try {
            InetAddress.getAllByName(addressField.text)
        } catch (UnknownHostException iox) {
            rv << "Not a valid InetAddress"
        }
        try {
            int port = Integer.parseInt(portField.text)
            if (port <= 0 && port > 0xFFFF)
                rv << "Not a valid port"
        } catch (NumberFormatException e) {
            rv << "Not a valid port"
        }
        rv
    }

    @Override
    protected void apply(MuWireSettings muSettings, Properties i2pSettings) {
        i2pSettings['i2cp.tcp.host'] = addressField.text
        i2pSettings['i2cp.tcp.port'] = portField.text
    }
}
