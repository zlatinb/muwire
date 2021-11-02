
application {
    title = 'MuWire'
    startupGroups = ['EventList', 'MainFrame', 'ShutdownWindow']
    autoShutdown = false
}

griffon.disable.threading.injection = true

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
    'ShutdownWindow' {
        model = 'com.muwire.gui.ShutdownWindowModel'
        view = 'com.muwire.gui.ShutdownWindowView'
        controller = 'com.muwire.gui.ShutdownWindowController'
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
    'system-status' {
        model = 'com.muwire.gui.SystemStatusModel'
        view = 'com.muwire.gui.SystemStatusView'
        controller = 'com.muwire.gui.SystemStatusController'
    }
    'trust-list' {
        model = 'com.muwire.gui.TrustListModel'
        view = 'com.muwire.gui.TrustListView'
        controller = 'com.muwire.gui.TrustListController'
    }
    'content-panel' {
        model = 'com.muwire.gui.ContentPanelModel'
        view = 'com.muwire.gui.ContentPanelView'
        controller = 'com.muwire.gui.ContentPanelController'
    }
    'show-comment' {
        model = 'com.muwire.gui.ShowCommentModel'
        view = 'com.muwire.gui.ShowCommentView'
        controller = 'com.muwire.gui.ShowCommentController'
    }
    'add-comment' {
        model = 'com.muwire.gui.AddCommentModel'
        view = 'com.muwire.gui.AddCommentView'
        controller = 'com.muwire.gui.AddCommentController'
    }
    'browse' {
        model = 'com.muwire.gui.BrowseModel'
        view = 'com.muwire.gui.BrowseView'
        controller = 'com.muwire.gui.BrowseController'
    }
    'close-warning' {
        model = 'com.muwire.gui.CloseWarningModel'
        view = 'com.muwire.gui.CloseWarningView'
        controller = 'com.muwire.gui.CloseWarningController'
    }
    'update' {
        model = 'com.muwire.gui.UpdateModel'
        view = 'com.muwire.gui.UpdateView'
        controller = 'com.muwire.gui.UpdateController'
    }
    'advanced-sharing' {
        model = 'com.muwire.gui.AdvancedSharingModel'
        view = 'com.muwire.gui.AdvancedSharingView'
        controller = 'com.muwire.gui.AdvancedSharingController'
    }
    'fetch-certificates' {
        model = 'com.muwire.gui.FetchCertificatesModel'
        view = 'com.muwire.gui.FetchCertificatesView'
        controller = 'com.muwire.gui.FetchCertificatesController'
    }
    'certificate-warning' {
        model = 'com.muwire.gui.CertificateWarningModel'
        view = 'com.muwire.gui.CertificateWarningView'
        controller = 'com.muwire.gui.CertificateWarningController'
    }
    'certificate-control' {
        model = 'com.muwire.gui.CertificateControlModel'
        view = 'com.muwire.gui.CertificateControlView'
        controller = 'com.muwire.gui.CertificateControlController'
    }
    'shared-file' {
        model = 'com.muwire.gui.SharedFileModel'
        view = 'com.muwire.gui.SharedFileView'
        controller = 'com.muwire.gui.SharedFileController'
    }
    'download-preview' {
        model = "com.muwire.gui.DownloadPreviewModel"
        view = "com.muwire.gui.DownloadPreviewView"
        controller = "com.muwire.gui.DownloadPreviewController"
    }
    'chat-server' {
        model = 'com.muwire.gui.ChatServerModel'
        view = 'com.muwire.gui.ChatServerView'
        controller = 'com.muwire.gui.ChatServerController'
    }
    'chat-room' {
        model = 'com.muwire.gui.ChatRoomModel'
        view = 'com.muwire.gui.ChatRoomView'
        controller = 'com.muwire.gui.ChatRoomController'
    }
    'chat-monitor' {
        model = 'com.muwire.gui.ChatMonitorModel'
        view = 'com.muwire.gui.ChatMonitorView'
        controller = 'com.muwire.gui.ChatMonitorController'
    }
    'feed-configuration' {
        model = 'com.muwire.gui.FeedConfigurationModel'
        view = 'com.muwire.gui.FeedConfigurationView'
        controller = 'com.muwire.gui.FeedConfigurationController'
    }
    'watched-directory' {
        model = 'com.muwire.gui.WatchedDirectoryModel'
        view = 'com.muwire.gui.WatchedDirectoryView'
        controller = 'com.muwire.gui.WatchedDirectoryController'
    }
    'sign' {
        model = 'com.muwire.gui.SignModel'
        view = 'com.muwire.gui.SignView'
        controller = 'com.muwire.gui.SignController'
    }
    'wizard' {
        model = 'com.muwire.gui.wizard.WizardModel'
        view = 'com.muwire.gui.wizard.WizardView'
        controller = 'com.muwire.gui.wizard.WizardController'
    }
    'collection-wizard' {
        model = 'com.muwire.gui.CollectionWizardModel'
        view = 'com.muwire.gui.CollectionWizardView'
        controller = 'com.muwire.gui.CollectionWizardController'
    }
    'collections-tool' {
        model = 'com.muwire.gui.CollectionsToolModel'
        view = 'com.muwire.gui.CollectionsToolView'
        controller = 'com.muwire.gui.CollectionsToolController'
    }
    'collection-tab' {
        model = 'com.muwire.gui.CollectionTabModel'
        view = 'com.muwire.gui.CollectionTabView'
        controller = 'com.muwire.gui.CollectionTabController'
    }
    'collection-warning' {
        model = "com.muwire.gui.CollectionWarningModel"
        view = "com.muwire.gui.CollectionWarningView"
        controller = "com.muwire.gui.CollectionWarningController"
    }
    'new-message' {
        model = "com.muwire.gui.NewMessageModel"
        view = "com.muwire.gui.NewMessageView"
        controller = "com.muwire.gui.NewMessageController"
    }
    'add-contact' {
        model = "com.muwire.gui.AddContactModel"
        view = "com.muwire.gui.AddContactView"
        controller = "com.muwire.gui.AddContactController"
    }
    'message-folder' {
        model = 'com.muwire.gui.MessageFolderModel'
        view = 'com.muwire.gui.MessageFolderView'
        controller = 'com.muwire.gui.MessageFolderController'
    }
    'my-feed' {
        model = 'com.muwire.gui.MyFeedModel'
        view = 'com.muwire.gui.MyFeedView'
        controller = 'com.muwire.gui.MyFeedController'
    }
    'result-details-tabs' {
        model = 'com.muwire.gui.resultdetails.ResultDetailsTabsModel'
        view = 'com.muwire.gui.resultdetails.ResultDetailsTabsView'
        controller = 'com.muwire.gui.resultdetails.ResultDetailsTabsController'
    }
    'certificate-tab' {
        model = 'com.muwire.gui.resultdetails.CertificateTabModel'
        view = 'com.muwire.gui.resultdetails.CertificateTabView'
        controller = 'com.muwire.gui.resultdetails.CertificateTabController'
    }
    'certificate-list' {
        model = 'com.muwire.gui.resultdetails.CertificateListModel'
        view = 'com.muwire.gui.resultdetails.CertificateListView'
        controller = 'com.muwire.gui.resultdetails.CertificateListController'
    }
    'mini-collection-tab' {
        model = 'com.muwire.gui.resultdetails.MiniCollectionTabModel'
        view = 'com.muwire.gui.resultdetails.MiniCollectionTabView'
        controller = 'com.muwire.gui.resultdetails.MiniCollectionTabController'
    }
    'collection-list' {
        model = 'com.muwire.gui.resultdetails.CollectionListModel'
        view = 'com.muwire.gui.resultdetails.CollectionListView'
        controller = 'com.muwire.gui.resultdetails.CollectionListController'
    }
}
