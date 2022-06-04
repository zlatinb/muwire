package com.muwire.gui.profile

import com.muwire.core.profile.MWProfile
import com.muwire.gui.HTMLSanitizer

import javax.imageio.ImageIO
import javax.swing.Icon
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextArea
import javax.swing.SwingUtilities
import javax.swing.border.TitledBorder
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Window
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

import static com.muwire.gui.Translator.trans

import griffon.core.GriffonApplication
import griffon.core.artifact.GriffonView
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.annotation.Nonnull
import javax.inject.Inject
import javax.swing.JFrame

@ArtifactProviderFor(GriffonView)
class ViewProfileView {
    @MVCMember @Nonnull
    ViewProfileModel model
    @MVCMember @Nonnull
    ViewProfileController controller
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @Inject
    GriffonApplication application

    JFrame window
    JFrame mainFrame

    JPanel mainPanel
    JPanel thumbnailPanel
    JLabel titleLabel
    JPanel imagePanel
    JTextArea bodyArea
    
    void initUI() {
        mainFrame = application.windowManager.findWindow("main-frame")
        def mainDim = mainFrame.getSize()
        
        int dimX = Math.max(1100, (int)(mainDim.getWidth() / 2))
        int dimY = Math.max(700, (int)(mainDim.getHeight() / 2))
        
        window = builder.frame(visible: false, defaultCloseOperation: JFrame.DISPOSE_ON_CLOSE,
                iconImage: builder.imageIcon("/MuWire-48x48.png").image,
                title: trans("PROFILE_VIEWER_TITLE", model.persona.getHumanReadableName())) {
            borderLayout()
            panel(border: titledBorder(title : trans("PROFILE_VIEWER_HEADER"), border: etchedBorder(),
                titlePosition: TitledBorder.TOP), constraints: BorderLayout.NORTH) {
                if (model.profileHeader == null)
                    thumbnailPanel = panel(preferredSize: [ProfileConstants.MAX_THUMBNAIL_SIZE, ProfileConstants.MAX_THUMBNAIL_SIZE])
                else {
                    Icon thumbNail = new ThumbnailIcon(model.profileHeader.getThumbNail())
                    thumbnailPanel = panel(icon: thumbNail.image)
                }
                if (model.profileTitle == null)
                    titleLabel = label(text: trans("PROFILE_VIEWER_HEADER_MISSING"))
                else
                    titleLabel = label(text: model.profileTitle)
            }
            mainPanel = panel(constraints: BorderLayout.CENTER) {
                cardLayout()
                panel(constraints: "fetch-profile") {
                    button(text: trans("PROFILE_VIEWER_FETCH"), toolTipText: trans("TOOLTIP_PROFILE_VIEWER_FETCH"), 
                            fetchAction)
                }
                panel(constraints: "full-profile") {
                    gridLayout(rows: 1, cols: 2)
                    panel(border: titledBorder(title: trans("PROFILE_VIEWER_AVATAR"), border: etchedBorder(),
                        titlePosition: TitledBorder.TOP)) {
                        gridLayout(rows: 1, cols: 1)
                        imagePanel = panel()
                    }
                    panel(border: titledBorder(title: trans("PROFILE_VIEWER_PROFILE"), border: etchedBorder(),
                        titlePosition: TitledBorder.TOP)) {
                        gridLayout(rows: 1, cols: 1)
                        scrollPane {
                            bodyArea = textArea(editable: false, lineWrap: true, wrapStyleWord: true)
                        }
                    }
                }
            }
            panel(constraints: BorderLayout.SOUTH) {
                gridLayout(rows:1, cols: 4)
                panel {
                    label(text : bind { model.status == null ? "" : trans(model.status.name())})
                }
                panel {
                    button(text: trans("COPY_FULL_ID"), copyFullAction)
                }
                panel {
                    button(text: trans("ADD_CONTACT"), toolTipText: trans("TOOLTIP_PROFILE_VIEWER_ADD_CONTACT"),
                            addContactAction)
                    button(text: trans("PROFILE_VIEWER_BLOCK"), toolTipText: trans("TOOLTIP_PROFILE_VIEWER_BLOCK"),
                            blockAction)
                }
                panel {
                    button(text : trans("CLOSE"), closeAction)
                }
            }
            
        }
        
        window.setPreferredSize([dimX, dimY] as Dimension)        
    }
    
    void mvcGroupInit(Map<String, String> params) {
        window.addWindowListener(new WindowAdapter() {
            @Override
            void windowClosed(WindowEvent e) {
                mvcGroup.destroy()
            }
        })
        
        window.pack()
        window.setLocationRelativeTo(mainFrame)
        window.setVisible(true)
    }
    
    void profileFetched(MWProfile profile) {
        mainPanel.getLayout().show(mainPanel, "full-profile")
        titleLabel.setText(HTMLSanitizer.sanitize(profile.getHeader().getTitle()))
        bodyArea.setText(profile.getBody())

        def rawImage = ImageIO.read(new ByteArrayInputStream(profile.getImage()))
        def mainImage = ImageScaler.scaleToMax(rawImage)

        SwingUtilities.invokeLater {
            
            Icon thumbNail = new ThumbnailIcon(profile.getHeader().getThumbNail())
            def thumbDim = thumbnailPanel.getSize()
            thumbnailPanel.getGraphics().drawImage(thumbNail.image,
                    (int)(thumbDim.getWidth() / 2) - (int)(thumbNail.getIconHeight() / 2),
                    (int)(thumbDim.getHeight() / 2) - (int)(thumbNail.getIconHeight() / 2),
                    null)
            
            def imgDim = imagePanel.getSize()
            imagePanel.getGraphics().drawImage(mainImage,
                    (int) (imgDim.getWidth() / 2) - (int) (mainImage.getWidth() / 2),
                    (int) (imgDim.getHeight() / 2) - (int) (mainImage.getHeight() / 2),
                    null)
        } as Runnable
    }
}
