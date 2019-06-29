
application {
    title = 'MuWire'
    startupGroups = ['EventList', 'MainFrame']
    autoShutdown = true
}

mvcGroups {
    'EventList' {
        model = 'com.muwire.gui.EventListModel'
        view = 'com.muwire.gui.EventListView'
        controller = 'com.muwire.gui.EventListController'
    }
    'MainFrame' {
        model = 'com.muwire.gui.MainFrameModel'
        view = 'com.muwire.gui.MainFrameView'
        controller = 'com.muwire.gui.MainFrameController'
    }
    'SearchTab' {
        model = 'com.muwire.gui.SearchTabModel'
        view = 'com.muwire.gui.SearchTabView'
        controller = 'com.muwire.gui.SearchTabController'
    }
    'Options' {
        model = 'com.muwire.gui.OptionsModel'
        view = 'com.muwire.gui.OptionsView'
        controller = 'com.muwire.gui.OptionsController'
    }
    "mu-wire-status" {
        model = 'com.muwire.gui.MuWireStatusModel'
        view = 'com.muwire.gui.MuWireStatusView'
        controller = 'com.muwire.gui.MuWireStatusController'
    }
    'i-2-p-status' {
        model = 'com.muwire.gui.I2PStatusModel'
        view = 'com.muwire.gui.I2PStatusView'
        controller = 'com.muwire.gui.I2PStatusController'
    }
}
