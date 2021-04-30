package com.muwire.gui.wizard

import java.awt.GridBagConstraints
import static com.muwire.gui.Translator.trans

import javax.swing.border.TitledBorder

import com.muwire.core.MuWireSettings

class EmbeddedRouterStep extends WizardStep {

    
    def udpPortField
    def tcpPortField
    
    def inBwField
    def outBwField
    
    public EmbeddedRouterStep(WizardDefaults defaults) {
        super("router", defaults)
    }

    @Override
    protected void buildUI(FactoryBuilderSupport builder, def nextAction) {
        builder.panel(constraints : getConstraint()) {
            gridBagLayout()
            panel(border : titledBorder(title : trans("PORT_SETTINGS"), border : etchedBorder(), titlePosition : TitledBorder.TOP,
            constraints : gbc(gridx: 0, gridy : 0, fill : GridBagConstraints.HORIZONTAL, weightx: 100))) {
                gridBagLayout()
                label(text : trans("TCP_PORT"), constraints : gbc(gridx :0, gridy: 0, anchor : GridBagConstraints.LINE_START, weightx : 100))
                tcpPortField = textField(text : String.valueOf(defaults.i2npTcpPort), columns : 4, constraints : gbc(gridx:1, gridy:0, anchor : GridBagConstraints.LINE_END))
                label(text : trans("UDP_PORT"), constraints : gbc(gridx :0, gridy: 1, anchor : GridBagConstraints.LINE_START, weightx : 100))
                udpPortField = textField(text : String.valueOf(defaults.i2npUdpPort), columns : 4, constraints : gbc(gridx:1, gridy:1, anchor : GridBagConstraints.LINE_END))
            }
            panel( border : titledBorder(title : trans("BANDWIDTH_SETTINGS"), border : etchedBorder(), titlePosition : TitledBorder.TOP),
            constraints : gbc(gridx : 0, gridy : 1, fill : GridBagConstraints.HORIZONTAL, weightx : 100)) {
                gridBagLayout()
                label(text : trans("INBOUND_BANDWIDTH"), constraints : gbc(gridx: 0, gridy : 0, anchor : GridBagConstraints.LINE_START, weightx : 100))
                inBwField = textField(text : String.valueOf(defaults.inBw), columns : 3, constraints : gbc(gridx : 1, gridy : 0, anchor : GridBagConstraints.LINE_END))
                label(text : trans("OUTBOUND_BANDWIDTH"), constraints : gbc(gridx: 0, gridy : 1, anchor : GridBagConstraints.LINE_START, weightx : 100))
                outBwField = textField(text : String.valueOf(defaults.outBw), columns : 3, constraints : gbc(gridx : 1, gridy : 1, anchor : GridBagConstraints.LINE_END))
            }
            panel (constraints : gbc(gridx: 0, gridy : 2, weighty: 100))
        }
    }

    @Override
    protected List<String> validate() {
        def rv = []
        try {
            int udpPort = Integer.parseInt(udpPortField.text)
            int tcpPort = Integer.parseInt(tcpPortField.text)
            if (udpPort <= 0 || udpPort > 0xFFFF)
                rv << trans("INVALID_UDP_PORT")
            if (tcpPort <= 0 || tcpPort > 0xFFFF)
                rv << trans("INVALID_TCP_PORT")
        } catch (NumberFormatException e) {
            rv << trans("INVALID_PORT")
        }
        try {
            int outBw = Integer.parseInt(outBwField.text)
            int inBw = Integer.parseInt(inBwField.text)
            if (outBw <= 0)
                rv << trans("OUT_CANNOT_BE_NEGATIVE")
            if (inBw <= 0)
                rv << trans("IN_CANNOT_BE_NEGATIVE")
        } catch (NumberFormatException e) {
            rv << trans("INVALID_BANDWIDTH")
        }
        
        rv
    }

    @Override
    protected void apply(MuWireSettings muSettings, Properties i2pSettings) {
        i2pSettings['i2np.ntcp.port'] = tcpPortField.text
        i2pSettings['i2np.udp.port'] = udpPortField.text
        muSettings.outBw = Integer.parseInt(outBwField.text)
        muSettings.inBw = Integer.parseInt(inBwField.text)
    }
}
