package com.muwire.gui.wizard

import com.muwire.core.Constants
import com.muwire.core.MuWireSettings
import com.muwire.core.util.DataUtil

class NicknameStep extends WizardStep {

    volatile def nickField
    
    public NicknameStep() {
        super("nickname", null)
    }

    @Override
    protected void buildUI(FactoryBuilderSupport builder) {
        builder.panel(constraints : getConstraint()) {
            label(text: "Select a nickname")
            nickField = textField(columns: 30)
        }
    }

    @Override
    protected List<String> validate() {
        String nickname = nickField.text
        if (nickname == null)
            return ['Please select a nickname']
        nickname = nickname.trim()
        if (nickname.length() == 0)
            return ['Nickname cannot be blank']
        if (!DataUtil.isValidName(nickname))
            return ["Nickname cannot contain any of ${Constants.INVALID_NICKNAME_CHARS} and must be no longer than ${Constants.MAX_NICKNAME_LENGTH} characters.  Choose another."]
        null
    }

    @Override
    protected void apply(MuWireSettings muSettings, Properties i2pSettings) {
        muSettings.nickname = nickField.text.trim()
    }
}
