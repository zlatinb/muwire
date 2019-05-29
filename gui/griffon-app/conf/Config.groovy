
application {
    title = 'MuWire'
    startupGroups = ['EventList']
    autoShutdown = true
}

mvcGroups {
    'EventList' {
        model = 'com.muwire.gui.EventListModel'
        view = 'com.muwire.gui.EventListView'
        controller = 'com.muwire.gui.EventListController'
    }
}