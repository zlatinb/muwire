package com.muwire.gui.profile

import com.muwire.core.Constants
import com.muwire.core.profile.MWProfileImageFormat
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
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.TransferHandler
import javax.swing.border.TitledBorder
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridBagConstraints
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
    JPanel imagePanel, thumbnailPanel
    JTextField titleField
    JTextArea bodyArea
    def mainPanel
    
    void initUI() {
        mainFrame = application.windowManager.findWindow("main-frame")
        
        int dimX = Math.max(1024, (int)(mainFrame.getWidth() / 2))
        int dimY = Math.max(600, (int)(mainFrame.getHeight() / 2))
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
            panel(constraints: BorderLayout.CENTER) {
                gridLayout(rows: 1, cols: 2)
                panel(border: titledBorder(title: trans("PROFILE_EDITOR_MAIN_IMAGE"), border: etchedBorder(),
                    titlePosition: TitledBorder.TOP), transferHandler: transferHandler) {
                    gridLayout(rows: 1, cols: 1)
                    imagePanel = panel(transferHandler: transferHandler)
                }
                panel {
                    gridBagLayout()
                    int gridY = 0
                    panel(border: titledBorder(title: trans("PROFILE_EDITOR_THUMBNAIL"), border: etchedBorder(),
                        titlePosition: TitledBorder.TOP), 
                            constraints: gbc(gridx: 0, gridy: gridY, weightx: 100, weighty: 10, fill: GridBagConstraints.BOTH)) {
                        gridLayout(rows:1, cols:1)
                        thumbnailPanel = panel()
                    }
                    gridY++
                    panel(border: titledBorder(title: trans("PROFILE_EDITOR_TITLE", Constants.MAX_PROFILE_TITLE_LENGTH), border: etchedBorder(),
                        titlePosition: TitledBorder.TOP), 
                            constraints: gbc(gridx: 0, gridy: gridY, weightx: 100, fill: GridBagConstraints.HORIZONTAL)) {
                        gridLayout(rows:1, cols:1)
                        titleField = textField()
                    }
                    gridY++
                    panel(border: titledBorder(title: trans("PROFILE_EDITOR_BODY", Constants.MAX_COMMENT_LENGTH), border: etchedBorder(),
                            titlePosition: TitledBorder.TOP), 
                            constraints: gbc(gridx: 0, gridy: gridY, weightx: 100, weighty: 100, fill: GridBagConstraints.BOTH)) {
                        gridLayout(rows:1, cols:1)
                        scrollPane {
                            bodyArea = textArea(editable: true, lineWrap: true, wrapStyleWord: true)
                        }
                    }
                }
            }
            panel (constraints: BorderLayout.SOUTH) {
                gridLayout(rows: 1, cols: 2)
                panel {
                    button(text: trans("PROFILE_EDITOR_GENERATE"), toolTipText: trans("TOOLTIP_PROFILE_EDITOR_GENERATE"),
                            generateAction)
                }
                panel {
                    button(text: trans("SAVE"), saveAction)
                    button(text: trans("CANCEL"), cancelAction)
                }
            }
        }
    }
    
    void mvcGroupInit(Map<String, String> args) {
        dialog.getContentPane().add(mainPanel)
        dialog.setLocationRelativeTo(mainFrame)
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE)
        dialog.show()
    }
    
    void setImageAndThumbnail(InputStream inputStream) {
        def rawImage = ImageIO.read(inputStream)
        def mainImage = ImageScaler.scaleToMax(rawImage)
        def thumbNail = ImageScaler.scaleToThumbnail(rawImage)
        
        def imgDim = imagePanel.getSize()
        imagePanel.getGraphics().clearRect(0,0,(int)imgDim.getWidth(), (int)imgDim.getHeight())
        imagePanel.getGraphics().drawImage(mainImage,
                (int)(imgDim.getWidth() / 2) - (int)(mainImage.getWidth() / 2),
                (int)(imgDim.getHeight() / 2) - (int)(mainImage.getHeight() / 2),
                null)
        
        def thumbDim = thumbnailPanel.getSize()
        thumbnailPanel.getGraphics().clearRect(0,0, (int)thumbDim.getWidth(), (int)thumbDim.getHeight())
        thumbnailPanel.getGraphics().drawImage(thumbNail, (int)(thumbDim.getWidth() / 2) - 12,
                (int)(thumbDim.getHeight() / 2) - 12, null)
        
        def baos = new ByteArrayOutputStream()
        ImageIO.write(thumbNail, "png", baos)
        model.thumbnailData = baos.toByteArray()
    }
    
    void setImageAndThumbnail(byte [] rawData) {
        setImageAndThumbnail(new ByteArrayInputStream(rawData))
        model.imageData = rawData
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
            String fileName = f.getName().toLowerCase()
            
            if (!(fileName.endsWith("jpg") || fileName.endsWith("jpeg") || fileName.endsWith("png")))
                return false
            
            if (f.length() > Constants.MAX_PROFILE_IMAGE_LENGTH)
                return false
            
            f.withInputStream {setImageAndThumbnail it}
            
            model.format = fileName.endsWith("png") ? MWProfileImageFormat.PNG : MWProfileImageFormat.JPG
            model.imageData = f.bytes
            
            return true
        }
    }
}
