package com.muwire.gui.wizard

import com.muwire.core.Constants
import static com.muwire.gui.Translator.trans
import com.muwire.core.MuWireSettings
import com.muwire.core.util.DataUtil

class NicknameStep extends WizardStep {

    volatile def nickField
    
    public NicknameStep() {
        super("nickname", null)
    }

    @Override
    protected void buildUI(FactoryBuilderSupport builder, def nextAction) {
        builder.panel(constraints : getConstraint()) {
            label(text: trans("SELECT_A_NICKNAME"))
            nickField = textField(columns: 30, action : nextAction)
        }
    }

    @Override
    protected List<String> validate() {
        String nickname = nickField.text
        if (nickname == null)
            return [trans('PLEASE_SELECT_A_NICKNAME')]
        nickname = nickname.trim()
        if (nickname.length() == 0)
            return [trans('NICKNAME_CANNOT_BE_BLANK')]
        if (!DataUtil.isValidName(nickname))
            return [trans('NICKNAME_CANNOT_CONTAIN', Constants.INVALID_NICKNAME_CHARS, Constants.MAX_NICKNAME_LENGTH)]
        null
    }

    @Override
    protected void apply(MuWireSettings muSettings, Properties i2pSettings) {
        muSettings.nickname = nickField.text.trim()
    }
}
