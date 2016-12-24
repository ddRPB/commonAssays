/**
 * Main panel for the NAb QC interface.
 */
Ext4.QuickTips.init();

Ext4.define('LABKEY.ext4.NabQCPanel', {

    extend: 'Ext.panel.Panel',

    border: false,

    header : false,

    layout : {
        type : 'card',
        deferredRender : true
    },

    alias: 'widget.labkey-nab-qc-panel',

    padding: 10,

    constructor: function (config)
    {
        this.callParent([config]);
    },

    initComponent: function ()
    {
        this.activeItem = 'selectionpanel';

        this.prevBtn = new Ext4.Button({text: 'Previous', disabled: true, scope: this, handler: function(){
            if (this.activeItem === 'confirmationpanel'){
                this.activeItem = 'selectionpanel';
                this.prevBtn.setDisabled(true);
                this.nextBtn.setText('Next');
            }
            this.getLayout().setActiveItem(this.activeItem);
        }});

        this.nextBtn = new Ext4.Button({text: 'Next', scope: this, handler: function(){
            if (this.activeItem === 'selectionpanel'){
                this.activeItem = 'confirmationpanel';
                this.prevBtn.setDisabled(false);
                this.nextBtn.setText('Finish');

                this.getLayout().setActiveItem(this.activeItem);
            }
            else {
                this.onSave();
            }
        }});

        this.cancelBtn = new Ext4.Button({text: 'Cancel', scope: this, handler: function(){
            window.location = LABKEY.ActionURL.buildURL('nabassay', 'details.view', null, {rowId : this.runId});
        }});

        if (this.edit)
            this.bbar = ['->', this.nextBtn, this.prevBtn, this.cancelBtn];
        else
            this.bbar = ['->', {text: 'Done', scope: this, handler: function(){
                window.location = LABKEY.ActionURL.buildURL('nabassay', 'details.view', null, {rowId : this.runId});
            }}];

        this.items = [];
        this.items.push(this.getSelectionPanel());
        this.items.push(this.getConfirmationPanel());

        this.callParent(arguments);
    },

    getSelectionPanel : function(){

        if (!this.selectionPanel){
            // create a placeholder component and swap it out after we get the QC information for the run
            this.selectionPanel = Ext4.create('Ext.panel.Panel', {
                itemId : 'selectionpanel',
                items : [{
                    xtype : 'panel',
                    height : 700

                }],
                listeners : {
                    scope   : this,
                    render  : function(cmp) {
                        cmp.getEl().mask('Requesting QC information');
                        this.getQCInformation();
                    }
                }
            });
        }
        return this.selectionPanel;
    },

    getQCInformation : function(){

        LABKEY.Ajax.request({
            url: LABKEY.ActionURL.buildURL('nabassay', 'getQCControlInfo.api'),
            method: 'POST',
            params: {
                rowId : this.runId
            },
            scope: this,
            success: function(response){
                var json = Ext4.decode(response.responseText);
                var items = [];
                this.plates = json.plates;
                var data = {
                    runName         : this.runName,
                    runProperties   : this.runProperties,
                    controls        : this.controlProperties,
                    plates          : this.plates,
                    dilutionSummaries  : json.dilutionSummaries
                };

                var tplText = [];

                tplText.push('<table class="qc-panel"><tr><td colspan="2">');
                tplText.push(this.getSummaryTpl());
/*
                tplText.push('</td></tr><tr><td valign="top">');
                tplText.push(this.getPlateControlsTpl());
                tplText.push('</td><td>');
                tplText.push(this.getControlWellsTpl());
*/
                tplText.push('</td></tr><tr><td colspan="2">');
                tplText.push(this.getDilutionSummaryTpl());
                tplText.push('</td></tr></table>');

                // create the template and add the template functions to the configuration
                items.push({
                    xtype : 'panel',
                    tpl : new Ext4.XTemplate(tplText.join(''),
                            {
                                getId : function(cmp) {
                                    return Ext4.id();
                                },
                                getColspan : function(cmp, values){
                                    return values.columnLabel.length / 2;
                                },
                                me : this
                            }
                    ),
                    data : data,
                    listeners : {
                        scope   : this,
                        render  : function(cmp) {
                            this.renderControls();
                        }
                    }
                });
                var selectionPanel = this.getSelectionPanel();
                selectionPanel.getEl().unmask();
                selectionPanel.removeAll();

                if (this.edit) {
                    selectionPanel.add(items);
                }
                else {
                    // create the store to get the initial load
                    var store = this.getFieldSelectionStore();
                    store.on('load', function(){
                        this.activeItem = 'confirmationpanel';
                        this.getLayout().setActiveItem(this.activeItem);
                    }, this, {single : true})
                }
            }
        });
    },

    /**
     * Generate the template for the run summary section
     * @returns {Array.<*>}
     */
    getSummaryTpl : function(){
        var tpl = [];
        tpl.push('<table>',
                '<tr class="labkey-data-region-header-container" style="text-align:center;"><th colspan="8">Run Summary: {runName}</th></tr>',
                '<tr><td>',
                '<tr>',
                '<tpl for="runProperties">',
                '<th style="text-align: left">{name}</th>',
                '<td>{value}</td>',
                '<tpl if="xindex % 2 === 0"></tr><tr></tpl>',
                '</tpl>',
                '</td></tr>',
                '</table>'
        );
        return tpl.join('');
    },

    getPlateControlsTpl : function(){
        var tpl = [];
        tpl.push('<table class="labkey-data-region labkey-show-borders">',
                '<tr><th></th><th>Plate</th><th>Range</th><th>Virus Control</th><th>Cell Control</th></tr>',
                '<tpl for="controls">',
                '<tr>',
                '<td class="plate-checkbox" plateNumm="{[xindex]}" id="{[this.getId(this)]}"></td>',
                '<td style="font-weight:bold">{[xindex]}</td>',
                '<td align="left">{controlRange}</td>',
                '<td align="left">{virusControlMean} &plusmn; {virusControlPlusMinus}</td>',
                '<td align="left">{cellControlMean} &plusmn; {cellControlPlusMinus}</td>',
                '</tr>',
                '</tpl>',
                '</table>'
        );
        return tpl.join('');
    },

    getControlWellsTpl : function(){
        var tpl = [];

        tpl.push(//'<table class="labkey-data-region labkey-show-borders">',
                '<tpl for="plates">',
                '<table>',
                '<tpl for="controls">',
                '<tr><th colspan="5" class="labkey-data-region-header-container" style="text-align:center;">{parent.plateName}</th></tr>',
                '<tr><td colspan="{[this.getColspan(this, values) + 1]}">Virus Control</td><td colspan="{[this.getColspan(this, values)]}">Cell Control</td></tr>',
                '<tr><td><div class="plate-columnlabel"></div></td>',
                '<tpl for="columnLabel">',
                '<td><div class="plate-columnlabel">{.}</div></td>',
                '</tpl>',
                '</tr>',
                '<tpl for="rows">',
                '<tr>',
                '<tpl for=".">',
                '<tpl if="xindex === 1"><td>{rowlabel}</td></tpl>',
                '<td class="control-checkbox" id="{[this.getId(this)]}" label="{value}"></td>',
                '</tpl>',           // end cols
                '</tr>',
                '</tpl>',           // end rows
                '</tpl>',           // end controls
                '</table>',
                '</tpl>'           // end plates
                //'</table>'
        );
        return tpl.join('');
    },

    /**
     * Create the template for the dilution curve selection UI
     */
    getDilutionSummaryTpl : function(){
        var tpl = [];

        tpl.push('<tpl for="dilutionSummaries">',
                '<table class="dilution-summary">',
                '<tr><td colspan="10" class="labkey-data-region-header-container" style="text-align:center;">{name}</td></tr>',
                '<tr><td><img src="{graphUrl}" height="300" width="425"></td>',
                    '<td valign="top"><table class="labkey-data-region">',
                        '<tr>',
                            '<td>{methodLabel}</td><td>{neutLabel}</td>',
                            '<td class="dilution-checkbox-addall" id="{[this.getId(this)]}" specimen="{name}"></td><td></td></tr>',
                        '<tpl for="dilutions">',
                        '<tr><td>{dilution}</td><td>{neut} &plusmn; {neutPlusMinus}</td>',
                            '<tpl for="wells">',
                            '<td class="dilution-checkbox" id="{[this.getId(this)]}" label="{value}" col="{col}" rowlabel="{rowlabel}" row="{row}" specimen="{parent.sampleName}" plate="{plateNum}"></td>',
                            '</tpl>',           // end wells
                        '</tr>',
                        '</tpl>',               // end dilutions
                    '</table></td></tr>',
                '</table>',
                '</tpl>'
        );
        return tpl.join('');
    },

    /**
     * Add check controls and register listeners
     */
    renderControls : function(){

        var addCheckboxEls = Ext4.DomQuery.select('td.plate-checkbox', this.getEl().dom);
        Ext4.each(addCheckboxEls, function(cmp)
        {
            if (cmp.hasAttribute('id')){

                var field = Ext4.create('widget.checkbox', {
                    renderTo : cmp.getAttribute('id')
                });

                // add a listener...
            }
        }, this);

        var addCheckboxEls = Ext4.DomQuery.select('td.control-checkbox', this.getEl().dom);
        Ext4.each(addCheckboxEls, function(cmp)
        {
            if (cmp.hasAttribute('id')){

                var field = Ext4.create('widget.checkbox', {
                    renderTo : cmp.getAttribute('id'),
                    boxLabel : cmp.getAttribute('label')
                });

                // add a listener...
            }
        }, this);

        var addCheckboxEls = Ext4.DomQuery.select('td.dilution-checkbox', this.getEl().dom);
        Ext4.each(addCheckboxEls, function(cmp)
        {
            if (cmp.hasAttribute('id')){

                var field = Ext4.create('widget.checkbox', {
                    renderTo : cmp.getAttribute('id'),
                    boxLabel : cmp.getAttribute('label'),
                    row      : cmp.getAttribute('row'),
                    rowlabel : cmp.getAttribute('rowlabel'),
                    col      : cmp.getAttribute('col'),
                    specimen : cmp.getAttribute('specimen'),
                    plate    : cmp.getAttribute('plate'),
                    listeners : {
                        scope   : this,
                        change  : function(cmp, newValue) {
                            var store = this.getFieldSelectionStore();
                            if (store){
                                var key = this.getKey(cmp);
                                var rec = store.findRecord('key', key);
                                if (rec && !newValue){
                                    store.remove(rec);
                                    // seriously?
                                    var fieldPanel = this.confirmationPanel.getComponent('field-selection-view');
                                    if (fieldPanel){
                                        fieldPanel.refresh();
                                    }
                                }
                                else if (newValue && !rec){
                                    // add the record to the store
                                    store.add({
                                        key     : key,
                                        plate   : cmp.plate,
                                        row     : cmp.row,
                                        rowlabel : cmp.rowlabel,
                                        col : cmp.col,
                                        specimen : cmp.specimen,
                                        excluded : newValue
                                    });
                                }
                            }
                        }
                    }
                });
            }
        }, this);

        var addCheckboxSelectAllEls = Ext4.DomQuery.select('td.dilution-checkbox-addall', this.getEl().dom);
        Ext4.each(addCheckboxSelectAllEls, function(cmp)
        {
            if (cmp.hasAttribute('id')){

                var field = Ext4.create('widget.checkbox', {
                    renderTo : cmp.getAttribute('id'),
                    boxLabel : 'Select all',
                    specimen : cmp.getAttribute('specimen'),
                    plate    : cmp.getAttribute('plate'),
                    listeners : {
                        scope   : this,
                        change  : function(cmp, newValue) {
                            // get all of the checkboxes with the same specimen and plate
                            var checkboxes = Ext4.ComponentQuery.query('checkbox[specimen=' + cmp.specimen + ']');
                            Ext4.each(checkboxes, function(ck){

                                ck.setValue(newValue);
                            }, this);
                        }
                    }
                });
            }
        }, this);
    },

    getKey : function(cmp){
        return cmp.plate + '-' + cmp.row + '-' + cmp.col;
    },

    getConfirmationPanel : function(){

        if (!this.confirmationPanel){

            var tplText = [];

            tplText.push('<table class="qc-panel"><tr><td colspan="2">');
            tplText.push(this.getSummaryTpl());
            tplText.push('</td></tr></table>');

            this.confirmationPanel = Ext4.create('Ext.panel.Panel', {
                itemId : 'confirmationpanel',
                items : [{
                    xtype : 'panel',
                    tpl : new Ext4.XTemplate(tplText.join('')),
                    data : {
                        runName         : this.runName,
                        runProperties   : this.runProperties
                    }
                },{
                    xtype   : 'dataview',
                    itemId  : 'field-selection-view',
                    border  : false,
                    frame   : false,
                    flex    : 1.2,
                    tpl     : this.getFieldSelectionTpl(),
                    width   : '100%',
                    autoScroll : true,
                    store   : this.getFieldSelectionStore(),
                    itemSelector :  'tr.field-exclusion',
                    disableSelection: true,
                    listeners : {
                        scope   : this,
                        itemclick : function(view, rec, item, idx, event){
                            if (event.target.getAttribute('class') == 'fa labkey-link fa-times') {
                                view.getStore().remove(rec);
                                this.setWellCheckbox(rec, false);
                            }
                        },
                        refresh : function(cmp){
                            var commentFieldEls = Ext4.DomQuery.select('input.field-exclusion-comment', this.getEl().dom);
                            Ext4.each(commentFieldEls, function(cmp) {
                                Ext4.EventManager.addListener(cmp, 'change', this.onCommentChange, this);
                            }, this);
                        },
                        itemadd : function(rec, idx, nodes){
                            Ext4.each(nodes, function(node){
                                this.addCommentFieldListener(node);
                            }, this);
                        },
                        itemupdate : function(rec, idx, node){
                            this.addCommentFieldListener(node);
                        }
                    }
                }],
                listeners : {
                    scope   : this,
                    render  : function(cmp) {
                        // add the raw plate information lazily because it depends on the initial ajax request for
                        // selection data
                        var plateTpl = [];
                        plateTpl.push('<table class="qc-panel"><tr><td colspan="2">');
                        plateTpl.push(this.getPlateTpl());
                        plateTpl.push('</td></tr></table>');

                        cmp.add({
                            xtype : 'panel',
                            tpl : new Ext4.XTemplate(plateTpl.join(''),
                                    {
                                        getKey : function(rec) {
                                            return rec.plate + '-' + rec.row + '-' + rec.col;
                                        }
                                    }
                            ),
                            data : {
                                plates : this.plates
                            },
                            listeners : {
                                scope   : this,
                                render : function(cmp){
                                    // need to pick up any selections made prior to component render
                                    var store = this.getFieldSelectionStore();
                                    if (store){
                                        store.each(function(rec){

                                            var key = this.getKey(rec.getData());
                                            this.setWellExclusion(key, true);

                                        }, this);
                                    }
                                }
                            }
                        });
                    }
                }
            });
        }
        return this.confirmationPanel;
    },

    getFieldSelectionTpl : function(){
        return new Ext4.XTemplate(
            '<table>',
            '<tr class="labkey-data-region-header-container" style="text-align:center;"><th colspan="5">Excluded Field Wells</th></tr>',
            '<tr><th></th><th>Row</th><th>Column</th><th>Specimen</th><th>Plate</th><th>Comment</th></tr>',
            '<tpl for=".">',
                '<tr class="field-exclusion">',
                '<td class="remove-exclusion" data-qtip="Click to delete">{[this.getDeleteIcon(values)]}</td>',
                '<td style="text-align: left">{rowlabel}</td>',
                '<td style="text-align: left">{col}</td>',
                '<td style="text-align: left">{specimen}</td>',
                '<td style="text-align: left">Plate {plate}</td>',
                '<td style="text-align: left"><input class="field-exclusion-comment" key="{[this.getKey(values)]}" {[this.getReadonly()]} type="text" name="comment" size="60" value="{comment}"></td>',
                '</tr>',
            '</tpl>',
            '</table>',
            {
                // don't show the remove icon if we aren't in edit mode
                getDeleteIcon : function(rec){
                    if (this.me.edit){
                        return '<div><span class="fa labkey-link fa-times"></span></div>';
                    }
                    return '';
                },
                getKey : function(rec){
                    return rec.plate + '-' + rec.row + '-' + rec.col;
                },
                getReadonly : function(){
                    return this.me.edit ? '' : 'readonly';
                },
                me : this
            }
        );
    },

    getPlateTpl : function(){
        var tpl = [];

        tpl.push(//'<table class="labkey-data-region labkey-show-borders">',
                '<tpl for="plates">',
                '<table>',
                '<tpl for="rawdata">',
                '<tr><th colspan="40" class="labkey-data-region-header-container" style="text-align:center;">{parent.plateName}</th></tr>',
                '<tr><td><div class="plate-columnlabel"></div></td>',
                '<tpl for="columnLabel">',
                '<td><div class="plate-columnlabel">{.}</div></td>',
                '</tpl>',
                '</tr>',
                '<tpl for="data">',
                '<tr>',
                '<tpl for=".">',
                '<tpl if="xindex === 1"><td>{rowlabel}</td></tpl>',
                '<td class="{[this.getKey(values)]}">{value}</td>',
                '</tpl>',           // end cols
                '</tr>',
                '</tpl>',           // end rows
                '</tpl>',           // end controls
                '</table>',
                '</tpl>'           // end plates
                //'</table>'
        );
        return tpl.join('');
    },

    getFieldSelectionStore : function() {

        if (!this.fieldSelectionStore) {

            if (!Ext4.ModelManager.isRegistered('LABKEY.Nab.SelectedFields')) {

                Ext4.define('LABKEY.Nab.SelectedFields', {
                    extend: 'Ext.data.Model',
                    proxy : {
                        type : 'ajax',
                        url : LABKEY.ActionURL.buildURL('nabassay', 'getExcludedWells.api'),
                        extraParams : {rowId : this.runId},
                        reader : { type : 'json', root : 'excluded' }
                    },
                    fields : [
                        {name: 'key'},
                        {name: 'row'},
                        {name: 'rowlabel', mapping : 'rowLabel'},
                        {name: 'col'},
                        {name: 'specimen'},
                        {name: 'plate'},
                        {name: 'comment'},
                        {name: 'excluded'}
                    ]
                });
            }
            this.fieldSelectionStore = Ext4.create('Ext.data.Store', {
                model : 'LABKEY.Nab.SelectedFields',
                autoLoad: true,
                pageSize: 10000,
                listeners : {
                    scope   : this,
                    add : function(store, records){
                        Ext4.each(records, function(rec){

                            var key = this.getKey(rec.getData());
                            this.setWellExclusion(key, true);
                        }, this);
                    },
                    remove : function(store, records){
                        Ext4.each(records, function(rec){

                            var key = this.getKey(rec.getData());
                            this.setWellExclusion(key, false);
                        }, this);
                    },
                    load : function(store, records){
                        Ext4.each(records, function(rec){

                            this.setWellCheckbox(rec, true);
                        }, this);
                    }
                }
            });
        }
        return this.fieldSelectionStore;
    },

    /**
     * mark the well as excluded (or clear) on the plate diagram
     */
    setWellExclusion : function(key, excluded){

        if (excluded) {
            this.applyStyleToClass(key, {backgroundColor: '#FF7A83'})
        }
        else {
            this.applyStyleToClass(key, {backgroundColor: 'white'})
        }
    },

    applyStyleToClass : function(cls, style) {

        var el = Ext4.select('.' + cls, true);
        if (el) {
            el.applyStyles(style);
            el.repaint();
        }
    },

    onSave : function(){

        var excluded = [];
        this.getFieldSelectionStore().each(function(rec){

            excluded.push({
                row : rec.get('row'),
                col : rec.get('col'),
                plate : rec.get('plate'),
                comment : rec.get('comment')
            });
        }, this);

        this.getEl().mask('Saving QC information');

        Ext4.Ajax.request({
            url: LABKEY.ActionURL.buildURL('nabassay', 'saveQCControlInfo.api'),
            method: 'POST',
            jsonData: {
                runId : this.runId,
                excluded : excluded
            },
            success: function (response) {
                window.location = LABKEY.ActionURL.buildURL('nabassay', 'details.view', null, {rowId : this.runId});
            },
            failure: this.failureHandler,
            scope: this
        });
    },

    failureHandler : function(response)
    {
        var msg = response.status == 403 ? response.statusText : Ext4.JSON.decode(response.responseText).exception;
        Ext4.Msg.show({
            title:'Error',
            msg: msg,
            buttons: Ext4.Msg.OK,
            icon: Ext4.Msg.ERROR
        });
    },

    /**
     * Set the checked state of the checkbox matching the location specified
     */
    setWellCheckbox : function(rec, checked){
        // get all of the checkboxes with the same specimen and plate
        var checkboxes = Ext4.ComponentQuery.query('checkbox[plate=' + rec.get('plate') + '][row=' + rec.get('row') + '][col=' + rec.get('col') + ']');
        Ext4.each(checkboxes, function(ck){

            ck.setValue(checked);
        }, this);

    },

    /**
     * Adds a change listener to the comment field and pushes any changes into the field selection store
     */
    addListenersToCommentFields : function(node, style) {

        // remove existing listeners

        var commentFieldEls = Ext4.DomQuery.select('input.field-exclusion-comment', this.getEl().dom);
        Ext4.each(commentFieldEls, function(cmp)
        {
            Ext4.EventManager.addListener(cmp, 'change', function(event, cmp){
            }, this);
        }, this);
    },

    addCommentFieldListener : function(node){
        Ext4.EventManager.addListener(node, 'change', this.onCommentChange, this, {single : true});
    },

    onCommentChange : function(event, cmp){
        var key = cmp.getAttribute('key');
        if (key){
            var store = this.getFieldSelectionStore()
            var rec = store.findRecord('key', key);
            if (rec && cmp.value){
                rec.set('comment', cmp.value);
            }
        }
    }
});