package com.muwire.gui.wizard

import java.awt.GridBagConstraints

import javax.swing.JLabel
import javax.swing.border.TitledBorder

import com.muwire.core.MuWireSettings

class TunnelStep extends WizardStep {

    
    def tunnelLengthSlider
    def tunnelQuantitySlider
    
    public TunnelStep() {
        super("tunnels")
    }

    @Override
    protected void buildUI(FactoryBuilderSupport builder) {
        builder.panel (constraints : getConstraint()) {
            gridBagLayout()
            panel (border : titledBorder(title : "Speed vs. Anonymity", border : etchedBorder(), titlePosition: TitledBorder.TOP,
                constraints : gbc(gridx: 0, gridy: 0, fill : GridBagConstraints.HORIZONTAL, weightx : 100))) {
                def lengthTable = new Hashtable()
                lengthTable.put(1, new JLabel("Max Speed"))
                lengthTable.put(3, new JLabel("Max Anonymity"))
                tunnelLengthSlider = slider(minimum : 1, maximum : 3, value : 3,
                    majorTickSpacing : 1, snapToTicks: true, paintTicks: true, labelTable : lengthTable,
                    paintLabels : true)
            }
            panel (border : titledBorder(title : "Reliability vs. Resource Usage", border : etchedBorder(), titlePosition: TitledBorder.TOP,
                constraints : gbc(gridx: 0, gridy: 1, fill : GridBagConstraints.HORIZONTAL, weightx : 100))) {
                def quantityTable = new Hashtable()
                quantityTable.put(1, new JLabel("Min Resources"))
                quantityTable.put(6, new JLabel("Max Reliability"))
                tunnelQuantitySlider = slider(minimum : 1, maximum : 6, value : 4,
                    majorTickSpacing : 1, snapToTicks : true, paintTicks: true, labelTable : quantityTable,
                    paintLabels : true)
            }
            panel(constraints : gbc(gridx:0, gridy: 2, weighty: 100))
        }
    }

    @Override
    protected List<String> validate() {
        return null
    }

    @Override
    protected void apply(MuWireSettings muSettings, Properties i2pSettings) {
        String tunnelLength = String.valueOf(tunnelLengthSlider.value)
        i2pSettings['inbound.length'] = tunnelLength
        i2pSettings['outbound.length'] = tunnelLength
        
        String tunnelQuantity = tunnelQuantitySlider.value
        i2pSettings['inbound.quantity'] = tunnelQuantity
        i2pSettings['outbound.quantity'] = tunnelQuantity
    }
}
