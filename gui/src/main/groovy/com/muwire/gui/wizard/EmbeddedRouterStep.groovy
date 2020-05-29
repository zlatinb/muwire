package com.muwire.gui.wizard

import java.awt.GridBagConstraints

import javax.swing.border.TitledBorder

import com.muwire.core.MuWireSettings

class EmbeddedRouterStep extends WizardStep {

    
    def udpPortField
    def tcpPortField
    
    def inBwField
    def outBwField
    
    public EmbeddedRouterStep() {
        super("router")
    }

    @Override
    protected void buildUI(FactoryBuilderSupport builder) {
        Random r = new Random()
        int port = 9151 + r.nextInt(1 + 30777 - 9151)  // this range matches what the i2p router would choose
        builder.panel(constraints : getConstraint()) {
            gridBagLayout()
            panel(border : titledBorder(title : "Port Settings", border : etchedBorder(), titlePosition : TitledBorder.TOP,
            constraints : gbc(gridx: 0, gridy : 0, fill : GridBagConstraints.HORIZONTAL, weightx: 100))) {
                gridBagLayout()
                label(text : "TCP port", constraints : gbc(gridx :0, gridy: 0, anchor : GridBagConstraints.LINE_START, weightx : 100))
                tcpPortField = textField(text : String.valueOf(port), columns : 4, constraints : gbc(gridx:1, gridy:0, anchor : GridBagConstraints.LINE_END))
                label(text : "UDP port", constraints : gbc(gridx :0, gridy: 1, anchor : GridBagConstraints.LINE_START, weightx : 100))
                udpPortField = textField(text : String.valueOf(port), columns : 4, constraints : gbc(gridx:1, gridy:1, anchor : GridBagConstraints.LINE_END))
            }
            panel( border : titledBorder(title : "Bandwidth Settings", border : etchedBorder(), titlePosition : TitledBorder.TOP),
            constraints : gbc(gridx : 0, gridy : 1, fill : GridBagConstraints.HORIZONTAL, weightx : 100)) {
                gridBagLayout()
                label(text : "Inbound bandwidth (KB)", constraints : gbc(gridx: 0, gridy : 0, anchor : GridBagConstraints.LINE_START, weightx : 100))
                inBwField = textField(text : "512", columns : 3, constraints : gbc(gridx : 1, gridy : 0, anchor : GridBagConstraints.LINE_END))
                label(text : "Outbound bandwidth (KB)", constraints : gbc(gridx: 0, gridy : 1, anchor : GridBagConstraints.LINE_START, weightx : 100))
                outBwField = textField(text : "256", columns : 3, constraints : gbc(gridx : 1, gridy : 1, anchor : GridBagConstraints.LINE_END))
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
                rv << "Invalid UDP Port"
            if (tcpPort <= 0 || tcpPort > 0xFFFF)
                rv << "Invalid TCP Port"
        } catch (NumberFormatException e) {
            rv << "Invalid port"
        }
        try {
            int outBw = Integer.parseInt(outBwField.text)
            int inBw = Integer.parseInt(inBwField.text)
            if (outBw <= 0)
                rv << "Out bandwidth cannot be negative"
            if (inBw <= 0)
                rv << "In bandwidth cannot be ngative"
        } catch (NumberFormatException e) {
            rv << "Invalid bandwidth"
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
