package com.muwire.gui

import griffon.core.GriffonApplication
import griffon.core.artifact.GriffonView
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.annotation.Nonnull
import javax.inject.Inject
import javax.swing.JDialog
import javax.swing.JFrame
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JProgressBar
import java.awt.BorderLayout
import static com.muwire.gui.Translator.trans

@ArtifactProviderFor(GriffonView)
class LibrarySyncView {

    @Inject @Nonnull
    GriffonApplication application
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @MVCMember @Nonnull
    LibrarySyncModel model
    @MVCMember @Nonnull
    LibrarySyncController controller

    private JFrame mainFrame

    JDialog scanDialog
    JDialog previewDialog
    JDialog reindexDialog

    private JProgressBar scanProgressBar
    JProgressBar reindexProgressBar

    void initUI() {
        mainFrame = (JFrame) application.windowManager.findWindow("main-frame")

        scanDialog = new JDialog(mainFrame, trans("LIBRARY_SCAN_TITLE"), true)

        JPanel scanPanel = builder.panel {
            borderLayout()
            panel(constraints: BorderLayout.NORTH) {
                label(text : trans("LIBRARY_SCAN_BODY"))
            }
            scanProgressBar = progressBar(constraints: BorderLayout.CENTER)
            panel(constraints: BorderLayout.SOUTH) {
                button(text: trans("CANCEL"), cancelScanAction)
            }
        }

        scanDialog.with {
            getContentPane().add(scanPanel)
            pack()
            setLocationRelativeTo(mainFrame)
            setDefaultCloseOperation(DISPOSE_ON_CLOSE)
        }
    }

    void mvcGroupInit(Map<String, String> args) {
        model.startScan()
        scanDialog.setVisible(true)
    }

    void updateScanProgressBar(int percent) {
        scanProgressBar.setValue(percent)
    }

    void scanCancelled() {
        scanDialog.setVisible(false)
    }

    void scanFinished() {

        if (model.staleFiles.isEmpty()) {
            JOptionPane.showMessageDialog(scanDialog, trans("LIBRARY_SCAN_NO_STALE"),
                trans("LIBRARY_SCAN_NO_STALE"), JOptionPane.INFORMATION_MESSAGE)
            scanDialog.setVisible(false)
            return
        }
    }
}
