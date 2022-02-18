package com.muwire.gui.tools

import com.muwire.core.content.MatchAction
import griffon.core.artifact.GriffonView
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.annotation.Nonnull
import javax.swing.JDialog
import javax.swing.JTextField
import javax.swing.border.TitledBorder
import java.awt.BorderLayout

import static com.muwire.gui.Translator.trans

@ArtifactProviderFor(GriffonView)
class RuleWizardView {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @MVCMember @Nonnull
    RuleWizardModel model
    
    def dialog
    def mainFrame
    def mainPanel

    JTextField nameField, termField
    
    void initUI() {
        mainFrame = application.windowManager.findWindow("main-frame")
        dialog = new JDialog(mainFrame, trans("RULE_WIZARD_TITLE"), true)
        
        mainPanel = builder.panel {
            borderLayout()
            panel(constraints: BorderLayout.CENTER) {
                gridLayout(rows: 4, cols: 1)
                panel(border: titledBorder(title: trans("RULE_NAME"), border: etchedBorder(),
                        titlePosition: TitledBorder.TOP)) {
                    gridLayout(rows:1, cols: 1)
                    nameField = textField(toolTipText: trans("TOOLTIP_RULE_NAME"))
                }
                panel(border: titledBorder(title: trans("RULE_TERM"), border: etchedBorder(),
                        titlePosition: TitledBorder.TOP)) {
                    gridLayout(rows:1, cols: 1)
                    termField = textField(toolTipText: trans("TOOLTIP_RULE_TERM"))
                }
                panel(border: titledBorder(title: trans("RULE_TYPE"), border: etchedBorder(),
                        titlePosition: TitledBorder.TOP)) {
                    buttonGroup(id: "ruleType")
                    radioButton(text : trans("KEYWORD"), toolTipText: trans("TOOLTIP_KEYWORD"),
                            selected: bind {!model.regex }, buttonGroup: ruleType, actionPerformed: clearRegex)
                    radioButton(text: trans("REGEX"), toolTipText: trans("TOOLTIP_REGEX"),
                            selected: bind { model.regex }, buttonGroup: ruleType, actionPerformed: setRegex)
                }
                panel(border: titledBorder(title: trans("RULE_ACTION"), border: etchedBorder(),
                        titlePosition: TitledBorder.TOP)) {
                    buttonGroup(id: "ruleAction")
                    radioButton(text: trans("RULE_RECORD"), toolTipText: trans("TOOLTIP_RULE_RECORD"),
                            selected: bind { model.action == MatchAction.RECORD },
                            buttonGroup: ruleAction, actionPerformed: actionRecord)
                    radioButton(text: trans("RULE_DROP"), toolTipText: trans("TOOLTIP_RULE_DROP"),
                            selected: bind { model.action == MatchAction.DROP },
                            buttonGroup: ruleAction, actionPerformed: actionDrop)
                    radioButton(text: trans("RULE_BLOCK"), toolTipText: trans("TOOLTIP_RULE_BLOCK"),
                            selected: bind { model.action == MatchAction.BLOCK },
                            buttonGroup: ruleAction, actionPerformed: actionBlock)
                }
            }
            panel (constraints: BorderLayout.SOUTH) {
                button(text: trans("SAVE"), saveAction)
                button(text: trans("CANCEL"), cancelAction)
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
    
    
    def setRegex = {
        model.regex = true
    }
    
    def clearRegex = {
        model.regex = false
    }
    
    def actionRecord = {
        model.action = MatchAction.RECORD
    }
    
    def actionDrop = {
        model.action = MatchAction.DROP
    }
    
    def actionBlock = {
        model.action = MatchAction.BLOCK
    }
}
