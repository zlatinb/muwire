package com.muwire.gui.wizard

import griffon.core.artifact.GriffonView

import javax.swing.AbstractAction
import javax.swing.Action
import javax.swing.JComponent
import javax.swing.KeyStroke

import static com.muwire.gui.Translator.trans
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.swing.JDialog
import javax.swing.SwingConstants

import java.awt.BorderLayout
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

import javax.annotation.Nonnull

@ArtifactProviderFor(GriffonView)
class WizardView {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @MVCMember @Nonnull
    WizardModel model
    @MVCMember @Nonnull
    WizardController controller

    def dialog
    def p
    def nextButton, previousButton
    def cancelButton, finishButton
    
    void initUI() {
        dialog = new JDialog(model.parent, "Setup Wizard", true)
        
        p = builder.panel {
            borderLayout() 
            panel (id : "cards-panel", constraints : BorderLayout.CENTER) {
                cardLayout()
                model.steps.each { 
                    it.buildUI(builder, nextAction)
                }
            }   
            panel (constraints : BorderLayout.SOUTH) {
                gridLayout(rows:1, cols:2)
                panel {
                    cancelButton = button(text : trans("CANCEL"), cancelAction)
                }
                panel {
                    previousButton = button(text : trans("PREVIOUS"), enabled : bind {model.previousButtonEnabled}, previousAction)
                    nextButton = button(text : trans("NEXT"), enabled : bind {model.nextButtonEnabled}, nextAction)
                    finishButton = button(text : trans("FINISH"), enabled : bind {model.finishButtonEnabled}, finishAction)
                }
            } 
        }
    }
    
    void updateLayout() {
        model.previousButtonEnabled = model.currentStep > 0
        model.nextButtonEnabled = model.steps.size() > (model.currentStep + 1)
        model.finishButtonEnabled = model.steps.size() == (model.currentStep + 1)
        
        String constraints = model.steps[model.currentStep].getConstraint()
        def cardsPanel = builder.getVariable("cards-panel")
        
        if (model.nextButtonEnabled)
            nextButton.requestFocus()
        else if (model.finishButtonEnabled)
            finishButton.requestFocus()
        
        cardsPanel.getLayout().show(cardsPanel, constraints)
    }
    
    void mvcGroupInit(Map<String,String> args) {
        def enter = KeyStroke.getKeyStroke("ENTER")
        
        nextButton.getInputMap().put(enter, "next")
        previousButton.getInputMap().put(enter, "previous")
        cancelButton.getInputMap().put(enter, "cancel")
        finishButton.getInputMap().put(enter, "finish")
        
        nextButton.getActionMap().put("next", { controller.next() } as AbstractAction)
        previousButton.getActionMap().put("previous", { controller.previous() } as AbstractAction)
        cancelButton.getActionMap().put("cancel", {controller.cancel()} as AbstractAction)
        finishButton.getActionMap().put("finish", {controller.finish()} as AbstractAction)
        
        dialog.getContentPane().add(p)
        dialog.pack()
        dialog.setLocationRelativeTo(model.parent)
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE)
        dialog.addWindowListener( new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                mvcGroup.destroy()
            }
        })
        dialog.show()
    }
    
    void hide() {
        dialog.setVisible(false)
    }
}