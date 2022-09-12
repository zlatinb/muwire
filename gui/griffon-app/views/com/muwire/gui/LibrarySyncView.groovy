package com.muwire.gui

import com.google.common.collect.Sets
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
import javax.swing.JTable
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

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
    private int rowHeight

    private JDialog scanDialog
    private JDialog previewDialog
    private JDialog reindexDialog

    private JDialog nextDialog

    private JProgressBar scanProgressBar
    JProgressBar reindexProgressBar

    private final CloseAdapter closeAdapter = new CloseAdapter()

    void initUI() {
        mainFrame = (JFrame) application.windowManager.findWindow("main-frame")
        rowHeight = (int)application.context.get("row-height")

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
            addWindowListener(closeAdapter)
            getContentPane().add(scanPanel)
            pack()
            setLocationRelativeTo(mainFrame)
            setDefaultCloseOperation(DISPOSE_ON_CLOSE)
        }
    }

    void mvcGroupInit(Map<String, String> args) {
        model.startScan()
        nextDialog = scanDialog
        while(nextDialog != null)
            nextDialog.setVisible(true)
    }

    void updateScanProgressBar(int percent) {
        scanProgressBar.setValue(percent)
    }

    void scanCancelled() {
        nextDialog = null
        scanDialog.setVisible(false)
    }

    void scanFinished() {

        scanDialog.setVisible(false)

        if (model.staleFiles.isEmpty()) {
            JOptionPane.showMessageDialog(mainFrame, trans("LIBRARY_SCAN_NO_STALE"),
                trans("LIBRARY_SCAN_NO_STALE"), JOptionPane.INFORMATION_MESSAGE)
            nextDialog = null
            return
        }

        previewDialog = new JDialog(mainFrame, trans("LIBRARY_SCAN_PREVIEW_TITLE"), true)
        nextDialog = previewDialog

        JTable staleFilesTable
        JPanel previewPanel = builder.panel {
            borderLayout()
            panel(constraints: BorderLayout.CENTER) {
                int rows = model.staleCollections.isEmpty() ? 1 : 2
                gridLayout(rows: rows, cols: 1)
                panel {
                    borderLayout()
                    panel (constraints: BorderLayout.NORTH) {
                        label(text: trans("LIBRARY_SCAN_PREVIEW_FILES_BODY"))
                    }
                    scrollPane(constraints: BorderLayout.CENTER) {
                        staleFilesTable = table(autoCreateRowSorter: true, rowHeight: rowHeight) {
                            tableModel(list: model.staleFiles) {
                                closureColumn(header : trans("LIBRARY_SCAN_PREVIEW_COLUMN_FILE"),
                                        type: String, read: {it.getCachedPath()})
                                closureColumn(header : trans("LIBRARY_SCAN_PREVIEW_COLUMN_SHARED"),
                                        type: Long, read: {it.getSharedTime()})
                                closureColumn(header: trans("LIBRARY_SCAN_PREVIEW_COLUMN_MODIFIED"),
                                        type: Long, read: {it.getFile().lastModified()})
                            }
                        }
                    }
                }
                if (rows > 1) {
                    panel {
                        borderLayout()
                        panel(constraints: BorderLayout.NORTH) {
                            label(text: trans("LIBRARY_SCAN_PREVIEW_COLLECTIONS_BODY"))
                        }
                        scrollPane(constraints: BorderLayout.CENTER) {
                            list(items: model.staleCollections.collect {it.getName()})
                        }
                    }
                }
            }
            panel(constraints: BorderLayout.SOUTH) {
                button(text: trans("LIBRARY_SCAN_PREVIEW_REINDEX"), reindexAction)
                button(text: trans("CANCEL"), cancelAction)
            }
        }

        TableUtil.dateColumn(staleFilesTable, 1)
        TableUtil.dateColumn(staleFilesTable, 2)
        staleFilesTable.setDefaultRenderer(Long.class, new DateRenderer())

        Dimension dimension = mainFrame.getSize()
        previewDialog.with {
            addWindowListener(closeAdapter)
            getContentPane().add(previewPanel)
            setSize((int)(dimension.getWidth() - 100), (int)(dimension.getHeight() - 100))
            setLocationRelativeTo(mainFrame)
            setDefaultCloseOperation(DISPOSE_ON_CLOSE)
        }
    }

    void previewCancelled() {
        nextDialog = null
        previewDialog.setVisible(false)
    }

    void startReindex() {
        previewDialog.setVisible(false)
        reindexDialog = new JDialog(mainFrame, trans("LIBRARY_SCAN_REINDEX_TITLE"), true)
        nextDialog = reindexDialog

        JPanel reindexPanel = builder.panel {
            borderLayout()
            panel(constraints: BorderLayout.NORTH) {
                label(text: trans("LIBRARY_SCAN_REINDEX_TITLE"))
            }
            reindexProgressBar = progressBar(constraints: BorderLayout.CENTER)
        }

        reindexDialog.with {
            addWindowListener(closeAdapter)
            getContentPane().add(reindexPanel)
            pack()
            setLocationRelativeTo(mainFrame)
            setDefaultCloseOperation(DISPOSE_ON_CLOSE)
        }
    }

    void updateReindexProgressBar(int value) {
        reindexProgressBar.setValue(value)
    }

    void reindexComplete() {
        nextDialog = null
        reindexDialog.setVisible(false)
    }

    private class CloseAdapter extends WindowAdapter {
        void windowClosed(WindowEvent e) {
            nextDialog?.setVisible(false)
            nextDialog = null
        }
    }
}
