package com.muwire.gui.wizard

import java.awt.GridBagConstraints
import static com.muwire.gui.Translator.trans

import javax.swing.border.TitledBorder

import com.muwire.core.MuWireSettings

class EmbeddedRouterStep extends WizardStep {

    boolean embeddedRouter
    
    def udpPortField
    def tcpPortField
    def useUPNPCheckbox
    
    def inBwField
    def outBwField
    
    // if the user chooses external...
    def addressField
    def portField
    
    def builder
    
    public EmbeddedRouterStep(WizardDefaults defaults) {
        super("router", defaults)
    }

    @Override
    protected void buildUI(FactoryBuilderSupport builder, def nextAction) {
        this.builder = builder
        builder.panel(constraints : getConstraint()) {
            gridBagLayout()
            panel(border: titledBorder(title: trans("OPTIONS_ROUTER_TYPE"), border: etchedBorder(), titlePosition: TitledBorder.TOP,
                    constraints: gbc(gridx: 0, gridy: 0, fill: GridBagConstraints.HORIZONTAL, weightx: 100))) {
                buttonGroup(id: "routerType")
                radioButton(text : trans("OPTIONS_EMBEDDED"), toolTipText: trans("TOOLTIP_OPTIONS_EMBEDDED"),
                        selected: bind {defaults.embeddedRouter}, buttonGroup: routerType, actionPerformed : switchToEmbedded)
                radioButton(text : trans("OPTIONS_EXTERNAL"), toolTipText: trans("TOOLTIP_OPTIONS_EXTERNAL"),
                        selected: bind {!defaults.embeddedRouter}, buttonGroup: routerType, actionPerformed : switchToExternal)
            }
            panel(id : "router-props", constraints: gbc(gridx: 0, gridy: 1, fill: GridBagConstraints.HORIZONTAL, weightx: 100)) {
                cardLayout()
                panel(constraints : "router-props-embedded") {
                    gridBagLayout()
                    panel(border: titledBorder(title: trans("PORT_SETTINGS"), border: etchedBorder(), titlePosition: TitledBorder.TOP,
                            constraints: gbc(gridx: 0, gridy: 0, fill: GridBagConstraints.HORIZONTAL, weightx: 100))) {
                        gridBagLayout()
                        label(text: trans("TCP_PORT"), constraints: gbc(gridx: 0, gridy: 0, anchor: GridBagConstraints.LINE_START, weightx: 100))
                        tcpPortField = textField(text: String.valueOf(defaults.i2npTcpPort), columns: 4, constraints: gbc(gridx: 1, gridy: 0, anchor: GridBagConstraints.LINE_END))
                        label(text: trans("UDP_PORT"), constraints: gbc(gridx: 0, gridy: 1, anchor: GridBagConstraints.LINE_START, weightx: 100))
                        udpPortField = textField(text: String.valueOf(defaults.i2npUdpPort), columns: 4, constraints: gbc(gridx: 1, gridy: 1, anchor: GridBagConstraints.LINE_END))
                        label(text: trans("USE_UPNP"), constraints: gbc(gridx: 0, gridy: 2, anchor: GridBagConstraints.LINE_START, weightx: 100))
                        useUPNPCheckbox = checkBox(selected: defaults.useUPNP, constraints: gbc(gridx: 1, gridy: 2, anchor: GridBagConstraints.LINE_END))
                    }
                    panel(border: titledBorder(title: trans("BANDWIDTH_SETTINGS"), border: etchedBorder(), titlePosition: TitledBorder.TOP),
                            constraints: gbc(gridx: 0, gridy: 1, fill: GridBagConstraints.HORIZONTAL, weightx: 100)) {
                        gridBagLayout()
                        label(text: trans("INBOUND_BANDWIDTH"), constraints: gbc(gridx: 0, gridy: 0, anchor: GridBagConstraints.LINE_START, weightx: 100))
                        inBwField = textField(text: String.valueOf(defaults.inBw), columns: 3, constraints: gbc(gridx: 1, gridy: 0, anchor: GridBagConstraints.LINE_END))
                        label(text: trans("OUTBOUND_BANDWIDTH"), constraints: gbc(gridx: 0, gridy: 1, anchor: GridBagConstraints.LINE_START, weightx: 100))
                        outBwField = textField(text: String.valueOf(defaults.outBw), columns: 3, constraints: gbc(gridx: 1, gridy: 1, anchor: GridBagConstraints.LINE_END))
                    }
                }
                panel(constraints: "router-props-external", border : titledBorder(title : trans("EXTERNAL_ROUTER_I2CP_SETTINGS"), border : etchedBorder(), titlePosition : TitledBorder.TOP,
                        constraints : gbc(gridx: 0, gridy : 0, fill : GridBagConstraints.HORIZONTAL, weightx: 100))) {
                    gridBagLayout()

                    label(text : trans("HOST"), constraints : gbc(gridx: 0, gridy : 0, anchor : GridBagConstraints.LINE_START, weightx : 100))
                    addressField = textField(text : defaults.i2cpHost, constraints : gbc(gridx:1, gridy:0, anchor: GridBagConstraints.LINE_END))

                    label(text : trans("PORT"), constraints : gbc(gridx: 0, gridy : 1, anchor : GridBagConstraints.LINE_START, weightx : 100))
                    portField = textField(text : String.valueOf(defaults.i2cpPort), constraints : gbc(gridx:1, gridy:1, anchor: GridBagConstraints.LINE_END))
                }
            }
            panel (constraints : gbc(gridx: 0, gridy : 2, weighty: 100))
        }
        
        if (defaults.embeddedRouter)
            switchToEmbedded.call()
        else
            switchToExternal.call()
    }
    
    def switchToEmbedded = {
        embeddedRouter = true
        def cardsPanel = builder.getVarialbe("router-props")
        cardsPanel.getLayout().show(cardsPanel, "router-props-embedded")
    }
    
    def switchToExternal = {
        embeddedRouter = false
        def cardsPanel = builder.getVarialbe("router-props")
        cardsPanel.getLayout().show(cardsPanel, "router-props-external")
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

        try {
            InetAddress.getAllByName(addressField.text)
        } catch (UnknownHostException iox) {
            rv << trans("INVALID_ADDRESS")
        }
        try {
            int port = Integer.parseInt(portField.text)
            if (port <= 0 && port > 0xFFFF)
                rv << trans("INVALID_PORT")
        } catch (NumberFormatException e) {
            rv << trans("INVALID_PORT")
        }
        
        rv
    }

    @Override
    protected void apply(MuWireSettings muSettings, Properties i2pSettings) {
        i2pSettings['i2cp.tcp.host'] = addressField.text
        i2pSettings['i2cp.tcp.port'] = portField.text
        
        i2pSettings['i2np.ntcp.port'] = tcpPortField.text
        i2pSettings['i2np.udp.port'] = udpPortField.text
        
        String upnp = String.valueOf(useUPNPCheckbox.model.isSelected())
        i2pSettings['i2np.upnp.enable'] = upnp
        i2pSettings['i2np.upnp.ipv6.enable'] = upnp
        
        muSettings.outBw = Integer.parseInt(outBwField.text)
        muSettings.inBw = Integer.parseInt(inBwField.text)
        
        muSettings.embeddedRouter = embeddedRouter
    }
}
