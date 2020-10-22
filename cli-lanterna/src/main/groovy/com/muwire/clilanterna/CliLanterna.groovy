package com.muwire.clilanterna

import java.nio.charset.StandardCharsets
import java.util.concurrent.CountDownLatch
import java.util.logging.Level
import java.util.logging.LogManager

import com.googlecode.lanterna.gui2.BasicWindow
import com.googlecode.lanterna.gui2.Border
import com.googlecode.lanterna.gui2.BorderLayout
import com.googlecode.lanterna.gui2.Borders
import com.googlecode.lanterna.gui2.Button
import com.googlecode.lanterna.gui2.GridLayout
import com.googlecode.lanterna.gui2.Label
import com.googlecode.lanterna.gui2.MultiWindowTextGUI
import com.googlecode.lanterna.gui2.Panel
import com.googlecode.lanterna.gui2.SeparateTextGUIThread
import com.googlecode.lanterna.gui2.Window
import com.googlecode.lanterna.gui2.WindowBasedTextGUI
import com.googlecode.lanterna.gui2.GridLayout.Alignment
import com.googlecode.lanterna.gui2.dialogs.MessageDialog
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton
import com.googlecode.lanterna.gui2.dialogs.TextInputDialog
import com.googlecode.lanterna.gui2.dialogs.TextInputDialogBuilder
import com.googlecode.lanterna.gui2.dialogs.WaitingDialog
import com.googlecode.lanterna.screen.Screen
import com.googlecode.lanterna.terminal.DefaultTerminalFactory
import com.googlecode.lanterna.terminal.Terminal
import com.muwire.core.Core
import com.muwire.core.MuWireSettings
import com.muwire.core.UILoadedEvent
import com.muwire.core.files.AllFilesLoadedEvent

class CliLanterna {
    private static final String MW_VERSION = "0.8.1"
    
    private static volatile Core core
    
    private static WindowBasedTextGUI textGUI
    
    public static void main(String[] args) {
        if (System.getProperty("java.util.logging.config.file") == null) {
            def names = LogManager.getLogManager().getLoggerNames()
            while(names.hasMoreElements()) {
                def name = names.nextElement()
                LogManager.getLogManager().getLogger(name).setLevel(Level.SEVERE)
            }
        }
        
        def home = System.getProperty("user.home") + File.separator + ".MuWire"
        home = new File(home)
        if (!home.exists())
            home.mkdirs()

        def propsFile = new File(home,"MuWire.properties")
        
        
        DefaultTerminalFactory terminalFactory = new DefaultTerminalFactory()
        Screen screen = terminalFactory.createScreen()
        textGUI = new MultiWindowTextGUI( new SeparateTextGUIThread.Factory(), screen)
        textGUI.getGUIThread().start()
        screen.startScreen()

        def props
        if (!propsFile.exists()) {
            String nickname = TextInputDialog.showDialog(textGUI, "Select a nickname", "", "")
            String defaultDownloadLocation = System.getProperty("user.home")+File.separator+"Downloads"
            String downloadLocation = TextInputDialog.showDialog(textGUI, "Select download location", "", defaultDownloadLocation)
            String defaultIncompletesLocation = System.getProperty("user.home")+File.separator+".MuWire"+File.separator+"incompletes"
            String incompletesLocation = TextInputDialog.showDialog(textGUI, "Select incompletes location", "", defaultIncompletesLocation)
            
            
            File downloadLocationFile = new File(downloadLocation)
            if (!downloadLocationFile.exists())
                downloadLocationFile.mkdirs()
            File incompletesLocationFile = new File(incompletesLocation)
            if (!incompletesLocationFile.exists())
                incompletesLocationFile.mkdirs()
            
            props = new MuWireSettings()
            props.setNickname(nickname)
            props.setDownloadLocation(downloadLocationFile)
            props.incompleteLocation = incompletesLocationFile
            
            propsFile.withPrintWriter("UTF-8", { 
                props.write(it)
            })
        } else {
            props = new Properties()
            propsFile.withReader("UTF-8", { 
                props.load(it)
            })
            props = new MuWireSettings(props)
        }
        props.updateType = "cli-lanterna"
                
        def i2pPropsFile = new File(home, "i2p.properties")
        if (!i2pPropsFile.exists()) {
            String i2pHost = TextInputDialog.showDialog(textGUI, "I2P router host", "Specifiy the host I2P router is on", "127.0.0.1")
            int i2pPort = TextInputDialog.showNumberDialog(textGUI, "I2CP port", "Specify the I2CP port", "7654").toInteger()
            
            Properties i2pProps = new Properties()
            i2pProps["i2cp.tcp.host"] = i2pHost
            i2pProps["i2cp.tcp.port"] = String.valueOf(i2pPort)
            i2pPropsFile.withOutputStream { i2pProps.store(it, "") }
        }
 
        def cliProps 
        def cliPropsFile = new File(home, "cli.properties")
        if (cliPropsFile.exists()) {
            Properties p = new Properties()
            cliPropsFile.withInputStream { 
                p.load(it)
            }
            cliProps = new CliSettings(p)
        } else
            cliProps = new CliSettings(new Properties())
               

        Window window = new BasicWindow("MuWire "+ MW_VERSION)
        window.setHints([Window.Hint.CENTERED])
        Panel contentPanel = new Panel()
        contentPanel.withBorder(Borders.doubleLine())
        BorderLayout layout = new BorderLayout()
        contentPanel.setLayoutManager(layout)
        
        Panel welcomeNamePanel = new Panel()
        contentPanel.addComponent(welcomeNamePanel, BorderLayout.Location.CENTER)
        welcomeNamePanel.setLayoutManager(new GridLayout(1))
        Label welcomeLabel = new Label("Welcome to MuWire "+ props.nickname)
        welcomeNamePanel.addComponent(welcomeLabel, GridLayout.createLayoutData(Alignment.CENTER, Alignment.CENTER))
        

        Panel connectButtonPanel = new Panel()
        contentPanel.addComponent(connectButtonPanel, BorderLayout.Location.BOTTOM)
        connectButtonPanel.setLayoutManager(new GridLayout(1))
        Button connectButton = new Button("Connect", {
            
            WaitingDialog waiting = new WaitingDialog("Connecting", "Please wait")
            waiting.showDialog(textGUI, false)
            
            CountDownLatch latch = new CountDownLatch(1)
            Thread connector = new Thread({
                try {
                    core = new Core(props, home, MW_VERSION)
                } finally {
                    latch.countDown()
                }
            })
            connector.start()
            while(latch.getCount() > 0) {
                textGUI.updateScreen()
                Thread.sleep(10)
            }
            waiting.close()
            window.close()
        } as Runnable)
        welcomeNamePanel.addComponent(connectButton, GridLayout.createLayoutData(Alignment.CENTER, Alignment.CENTER))
        
        
        window.setComponent(contentPanel)
        textGUI.addWindowAndWait(window)

        if (core == null) {
            MessageDialog.showMessageDialog(textGUI, "Failed", "MuWire failed to load", MessageDialogButton.Close)
            System.exit(1)
        }        
        
        window = new MainWindowView("MuWire "+MW_VERSION, core, textGUI, screen, cliProps)
        core.startServices()
        
        core.eventBus.publish(new UILoadedEvent())
        textGUI.addWindowAndWait(window)
        
        CountDownLatch latch = new CountDownLatch(1)
        Thread stopper = new Thread({
            core.shutdown()
            latch.countDown()
        } as Runnable)
        WaitingDialog waitingForShutdown = new WaitingDialog("MuWire is shutting down","Please wait")
        waitingForShutdown.setHints([Window.Hint.CENTERED])
        waitingForShutdown.showDialog(textGUI, false)
        stopper.start()
        while(latch.getCount() > 0) {
            textGUI.updateScreen()
            Thread.sleep(10)
        }
        waitingForShutdown.close()
        
        screen.stopScreen()
        System.exit(0)
    }
}
