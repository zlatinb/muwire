package com.muwire.gui.profile

import com.muwire.core.profile.MWProfile
import com.muwire.core.profile.MWProfileFetchStatus
import com.muwire.core.trust.TrustLevel
import com.muwire.gui.HTMLSanitizer

import javax.imageio.ImageIO
import javax.swing.Icon
import javax.swing.ImageIcon
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
    ImagePanel thumbnailPanel
    JLabel titleLabel
    ImagePanel imagePanel
    JTextArea bodyArea

    private Icon trusted, neutral, distrusted
    
    void initUI() {
        mainFrame = application.windowManager.findWindow("main-frame")
        def mainDim = mainFrame.getSize()
        
        int dimX = Math.max(1100, (int)(mainDim.getWidth() / 2))
        int dimY = Math.max(700, (int)(mainDim.getHeight() / 2))
        
        thumbnailPanel = new ImagePanel()
        thumbnailPanel.setPreferredSize([ProfileConstants.MAX_THUMBNAIL_SIZE, ProfileConstants.MAX_THUMBNAIL_SIZE] as Dimension)
        imagePanel = new ImagePanel()

        trusted = new ImageIcon(getClass().getClassLoader().getResource("trusted.png"))
        neutral = new ImageIcon(getClass().getClassLoader().getResource("neutral.png"))
        distrusted = new ImageIcon(getClass().getClassLoader().getResource("distrusted.png"))
        
        window = builder.frame(visible: false, defaultCloseOperation: JFrame.DISPOSE_ON_CLOSE,
                iconImage: builder.imageIcon("/MuWire-48x48.png").image,
                title: trans("PROFILE_VIEWER_TITLE", model.persona.getHumanReadableName())) {
            borderLayout()
            panel(border: titledBorder(title : trans("PROFILE_VIEWER_HEADER"), border: etchedBorder(),
                titlePosition: TitledBorder.TOP), constraints: BorderLayout.NORTH) {
                widget(thumbnailPanel)
                if (model.profileTitle == null)
                    titleLabel = label(text: trans("PROFILE_VIEWER_HEADER_MISSING"))
                else
                    titleLabel = label(text: model.profileTitle)
            }
            mainPanel = panel(constraints: BorderLayout.CENTER) {
                cardLayout()
                panel(constraints: "fetch-profile") {
                    button(text: trans("PROFILE_VIEWER_FETCH"), toolTipText: trans("TOOLTIP_PROFILE_VIEWER_FETCH"), 
                            enabled: bind {model.fetchEnabled()}, 
                            fetchAction)
                }
                panel(constraints: "full-profile") {
                    borderLayout()
                    panel(constraints: BorderLayout.CENTER) {
                        gridLayout(rows: 1, cols: 2)
                        panel(border: titledBorder(title: trans("PROFILE_VIEWER_AVATAR"), border: etchedBorder(),
                                titlePosition: TitledBorder.TOP)) {
                            gridLayout(rows: 1, cols: 1)
                            widget(imagePanel)
                        }
                        panel(border: titledBorder(title: trans("PROFILE_VIEWER_PROFILE"), border: etchedBorder(),
                                titlePosition: TitledBorder.TOP)) {
                            gridLayout(rows: 1, cols: 1)
                            scrollPane {
                                bodyArea = textArea(editable: false, lineWrap: true, wrapStyleWord: true)
                            }
                        }
                    }
                    panel(constraints: BorderLayout.SOUTH) {
                        button(text: trans("PROFILE_VIEWER_UPDATE"), toolTipText: trans("TOOLTIP_PROFILE_VIEWER_UPDATE"),
                            enabled: bind { model.fetchEnabled()}, fetchAction)
                    }
                }
            }
            panel(constraints: BorderLayout.SOUTH) {
                gridLayout(rows:1, cols: 4)
                panel {
                    borderLayout()
                    panel(constraints: BorderLayout.WEST) {
                        label(text: "", icon: bind{ getIcon(model.trustLevel) } ,
                                toolTipText: bind {trans(model.trustLevel.name())})
                    }
                    panel(constraints: BorderLayout.EAST) {
                        label(text: bind { model.status == null ? "" : trans(model.status.name()) })
                    }
                }
                panel {
                    button(text: trans("COPY_FULL_ID"), copyFullAction)
                }
                panel {
                    button(text: trans("ADD_CONTACT"), toolTipText: trans("TOOLTIP_PROFILE_VIEWER_ADD_CONTACT"),
                            enabled: bind {model.trustLevel != TrustLevel.TRUSTED}, addContactAction)
                    button(text: trans("PROFILE_VIEWER_BLOCK"), toolTipText: trans("TOOLTIP_PROFILE_VIEWER_BLOCK"),
                            enabled: bind {model.trustLevel != TrustLevel.DISTRUSTED}, blockAction)
                }
                panel {
                    button(text : trans("CLOSE"), closeAction)
                }
            }
            
        }
        window.setPreferredSize([dimX, dimY] as Dimension)        
    }
    
    private Icon getIcon(TrustLevel level) {
        switch(level) {
            case TrustLevel.TRUSTED : return trusted
            case TrustLevel.NEUTRAL : return neutral
            case TrustLevel.DISTRUSTED : return distrusted
        }
    }
    void mvcGroupInit(Map<String, String> params) {
        window.addWindowListener(new WindowAdapter() {
            @Override
            void windowOpened(WindowEvent e) {
                window.requestFocus()
                if (model.profileHeader != null) {
                    Icon thumbNail = new ThumbnailIcon(model.profileHeader.getThumbNail())
                    drawThumbnail(thumbNail)
                }
                if (model.profile != null)
                    profileFetched(model.profile)
            } 
            
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

        imagePanel.setImage(mainImage)
        SwingUtilities.invokeLater { // for some reason linux needs this.
            imagePanel.repaint()
        }
    }
    
    private void drawThumbnail(ThumbnailIcon thumbNail) {
        thumbnailPanel.setImage(thumbNail.image)
        thumbnailPanel.repaint()
    }
}
