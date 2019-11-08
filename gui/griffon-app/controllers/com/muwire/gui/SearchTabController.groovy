package com.muwire.gui

import griffon.core.artifact.GriffonController
import griffon.core.controller.ControllerAction
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import net.i2p.data.Base64

import javax.annotation.Nonnull
import javax.swing.JOptionPane

import com.muwire.core.Core
import com.muwire.core.Persona
import com.muwire.core.download.UIDownloadEvent
import com.muwire.core.search.UIResultEvent
import com.muwire.core.trust.TrustEvent
import com.muwire.core.trust.TrustLevel

@ArtifactProviderFor(GriffonController)
class SearchTabController {

    @MVCMember @Nonnull
    SearchTabModel model
    @MVCMember @Nonnull
    SearchTabView view

    Core core

    private def selectedResults() {
        int[] rows = view.resultsTable.getSelectedRows()
        if (rows.length == 0)
            return null
        def sortEvt = view.lastSortEvent
        if (sortEvt != null) {
            for (int i = 0; i < rows.length; i++) {
                rows[i] = view.resultsTable.rowSorter.convertRowIndexToModel(rows[i])
            }
        }
        List<UIResultEvent> results = new ArrayList<>()
        rows.each { results.add(model.results[it]) }
            results
        }

        @ControllerAction
        void download() {
            def results = selectedResults()
            if (results == null)
                return

            results.removeAll {
                !mvcGroup.parentGroup.model.canDownload(it.infohash)
            }

            results.each { result ->
                def file = new File(application.context.get("muwire-settings").downloadLocation, result.name)

                def resultsBucket = model.hashBucket[result.infohash]
                def sources = model.sourcesBucket[result.infohash]

                core.eventBus.publish(new UIDownloadEvent(result : resultsBucket, sources: sources,
                target : file, sequential : view.sequentialDownloadCheckbox.model.isSelected()))
            }
            mvcGroup.parentGroup.view.showDownloadsWindow.call()
        }

        @ControllerAction
        void trust() {
            int row = view.selectedSenderRow()
            if (row < 0)
                return
            String reason = JOptionPane.showInputDialog("Enter reason (optional)")
            def sender = model.senders[row]
            core.eventBus.publish( new TrustEvent(persona : sender, level : TrustLevel.TRUSTED, reason : reason))
        }

        @ControllerAction
        void distrust() {
            int row = view.selectedSenderRow()
            if (row < 0)
                return
            String reason = JOptionPane.showInputDialog("Enter reason (optional)")
            def sender = model.senders[row]
            core.eventBus.publish( new TrustEvent(persona : sender, level : TrustLevel.DISTRUSTED, reason : reason))
        }

        @ControllerAction
        void neutral() {
            int row = view.selectedSenderRow()
            if (row < 0)
                return
            def sender = model.senders[row]
            core.eventBus.publish( new TrustEvent(persona : sender, level : TrustLevel.NEUTRAL))
        }
        
        @ControllerAction
        void browse() {
            int selectedSender = view.selectedSenderRow()
            if (selectedSender < 0)
                return
            Persona sender = model.senders[selectedSender]
            
            String groupId = sender.getHumanReadableName()
            Map<String,Object> params = new HashMap<>()
            params['host'] = sender
            params['core'] = core
            
            mvcGroup.createMVCGroup("browse", groupId, params)
        }
        
        @ControllerAction
        void showComment() {
            int[] selectedRows = view.resultsTable.getSelectedRows()
            if (selectedRows.length != 1)
                return
            if (view.lastSortEvent != null)
                selectedRows[0] = view.resultsTable.rowSorter.convertRowIndexToModel(selectedRows[0])
            UIResultEvent event = model.results[selectedRows[0]]
            if (event.comment == null)
                return
                
            String groupId = Base64.encode(event.infohash.getRoot())
            Map<String,Object> params = new HashMap<>()
            params['text'] = event.comment
            params['name'] = event.name
            
            mvcGroup.createMVCGroup("show-comment", groupId, params)
        }
        
        @ControllerAction
        void viewCertificates() {
            int[] selectedRows = view.resultsTable.getSelectedRows()
            if (selectedRows.length != 1)
                return
            if (view.lastSortEvent != null)
                selectedRows[0] = view.resultsTable.rowSorter.convertRowIndexToModel(selectedRows[0])
            UIResultEvent event = model.results[selectedRows[0]]
            if (event.certificates <= 0)
                return
            
            def params = [:]
            params['result'] = event
            params['core'] = core
            mvcGroup.createMVCGroup("fetch-certificates", params)
        }
    }