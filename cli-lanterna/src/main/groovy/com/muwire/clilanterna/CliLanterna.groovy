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

class CliLanterna {
    private static final String MW_VERSION = "0.5.3"
    
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
            
            propsFile.withOutputStream { 
                props.write(it)
            }
        } else {
            props = new Properties()
            propsFile.withInputStream { 
                props.load(it)
            }
            props = new MuWireSettings(props)
        }
        
        

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
                core = new Core(props, home, MW_VERSION)
                core.startServices()
                latch.countDown()
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
        textGUI.getGUIThread().start()
        textGUI.addWindowAndWait(window)

        if (core == null) {
            MessageDialog.showMessageDialog(textGUI, "Failed", "MuWire failed to load", MessageDialogButton.Close)
            System.exit(1)
        }        
        
        window = new MainWindowView("MuWire "+MW_VERSION, core, textGUI)
        core.eventBus.publish(new UILoadedEvent())
        textGUI.addWindowAndWait(window)
        core.shutdown()
        screen.stopScreen()
        System.exit(0)
    }
}
