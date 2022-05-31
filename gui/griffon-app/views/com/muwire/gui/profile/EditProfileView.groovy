package com.muwire.gui.profile

import com.muwire.core.Constants
import griffon.core.GriffonApplication
import griffon.core.artifact.GriffonView
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.annotation.Nonnull
import javax.imageio.ImageIO
import javax.inject.Inject
import javax.swing.JComponent
import javax.swing.JDialog
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.TransferHandler
import javax.swing.border.TitledBorder
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.datatransfer.DataFlavor
import java.awt.image.BufferedImage

import static com.muwire.gui.Translator.trans

@ArtifactProviderFor(GriffonView)
class EditProfileView {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @MVCMember @Nonnull
    EditProfileModel model
    @MVCMember @Nonnull
    EditProfileController controller
    @Inject
    GriffonApplication application

    JFrame mainFrame
    JDialog dialog
    JPanel imagePanel
    def mainPanel
    
    void initUI() {
        mainFrame = application.windowManager.findWindow("main-frame")
        
        int dimX = Math.max(700, (int)(mainFrame.getWidth() / 2))
        int dimY = Math.max(700, (int)(mainFrame.getHeight() / 2))
        dialog = new JDialog(mainFrame, "Profile Editor", true)
        dialog.setSize([dimX, dimY] as Dimension)
        dialog.setResizable(false)
        
        def transferHandler = new ImageTransferHandler()
        
        mainPanel = builder.panel {
            borderLayout()
            panel ( border: titledBorder(title: trans("PROFILE_EDITOR_MUWIRE_ID"), border: etchedBorder(),
                titlePosition: TitledBorder.TOP), constraints: BorderLayout.NORTH) {
                label(text: model.core.me.getHumanReadableName(), constraints: BorderLayout.NORTH)
                button(text : trans("COPY_SHORT"), toolTipText: trans("TOOLTIP_COPY_SHORT_ID"),
                        copyShortAction)
                button(text : trans("COPY_FULL"), toolTipText: trans("TOOLTIP_COPY_FULL_ID"), 
                        copyFullAction)
            }
            panel(constraints: BorderLayout.CENTER, transferHandler: transferHandler) {
                gridLayout(rows: 1, cols: 1)
                imagePanel = panel()
            }
            panel (constraints: BorderLayout.SOUTH) {
                button(text : trans("SAVE"), saveAction)
                button(text : trans("CANCEL"), cancelAction)
            }
        }
    }
    
    void mvcGroupInit(Map<String, String> args) {
        dialog.getContentPane().add(mainPanel)
        dialog.setLocationRelativeTo(mainFrame)
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE)
        dialog.show()
    }
    
    void setImageAndThumbnail(BufferedImage image) {
        def mainImage = ImageScaler.scaleToMax(image)
        imagePanel.getGraphics().drawImage(image, 0, 0,null)
    }
    
    private class ImageTransferHandler extends TransferHandler {
        boolean canImport(TransferHandler.TransferSupport support) {
            support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)
        }
        
        boolean importData(TransferHandler.TransferSupport support) {
            def files = support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor)
            if (files == null) 
                return false
            if (files.size() != 1)
                return false
            File f = files[0]
            if (!(f.getName().endsWith("jpg") || f.getName().endsWith("jpeg") || f.getName().endsWith("png")))
                return false
            if (f.length() > Constants.MAX_PROFILE_IMAGE_LENGTH)
                return false
            setImageAndThumbnail(ImageIO.read(f))
            return true
        }
    }
}
