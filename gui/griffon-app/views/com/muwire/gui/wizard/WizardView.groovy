package com.muwire.gui.wizard

import griffon.core.artifact.GriffonView
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

    def dialog
    def p
    
    void initUI() {
        dialog = new JDialog(model.parent, "Setup Wizard", true)
        
        p = builder.panel {
            borderLayout() 
            panel (id : "cards-panel", constraints : BorderLayout.CENTER) {
                cardLayout()
                model.steps.each { 
                    it.buildUI(builder)
                }
            }   
            panel (constraints : BorderLayout.SOUTH) {
                gridLayout(rows:1, cols:2)
                panel {
                    button(text : trans("CANCEL"), cancelAction)
                }
                panel {
                    button(text : trans("PREVIOUS"), enabled : bind {model.previousButtonEnabled}, previousAction)
                    button(text : trans("NEXT"), enabled : bind {model.nextButtonEnabled}, nextAction)
                    button(text : trans("FINISH"), enabled : bind {model.finishButtonEnabled}, finishAction)
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
        cardsPanel.getLayout().show(cardsPanel, constraints)
    }
    
    void mvcGroupInit(Map<String,String> args) {
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
        mvcGroup.destroy()
    }
}