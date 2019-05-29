package com.muwire.gui

import griffon.core.test.GriffonUnitRule
import griffon.core.test.TestFor
import org.junit.Rule
import org.junit.Test

import static org.junit.Assert.fail

@TestFor(MainFrameController)
class MainFrameControllerTest {
    private MainFrameController controller

    @Rule
    public final GriffonUnitRule griffon = new GriffonUnitRule()

    @Test
    void smokeTest() {
        fail('Not yet implemented!')
    }
}