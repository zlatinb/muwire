package com.muwire.gui.profile

import griffon.core.GriffonApplication
import griffon.core.artifact.GriffonView
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.annotation.Nonnull
import javax.inject.Inject
import javax.swing.JDialog
import javax.swing.border.TitledBorder
import java.awt.BorderLayout

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
    
    def mainFrame
    def dialog
    def mainPanel
    
    void initUI() {
        mainFrame = application.windowManager.findWindow("main-frame")
        dialog = new JDialog(mainFrame, "Profile Editor", true)
        dialog.setResizable(false)
        
        mainPanel = builder.panel {
            borderLayout()
            panel ( border: titledBorder(title: trans("PROFILE_EDITOR_MUWIRE_ID"), border: etchedBorder(),
                titlePosition: TitledBorder.TOP), constraints: BorderLayout.NORTH) {
                borderLayout()
                label(text: model.core.me.getHumanReadableName(), constraints: BorderLayout.NORTH)
                panel(constraints: BorderLayout.SOUTH) {
                    button(text : trans("COPY_SHORT"), toolTipText: trans("TOOLTIP_COPY_SHORT_ID"),
                            copyShortAction)
                    button(text : trans("COPY_FULL"), toolTipText: trans("TOOLTIP_COPY_FULL_ID"), 
                            copyFullAction)
                }
            }
        }
    }
    
    void mvcGroupInit(Map<String, String> args) {
        dialog.getContentPane().add(mainPanel)
        dialog.pack()
        dialog.setLocationRelativeTo(mainFrame)
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE)
        dialog.show()
    }
}
