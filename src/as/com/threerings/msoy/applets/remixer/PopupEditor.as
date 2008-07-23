//
// $Id$

package com.threerings.msoy.applets.remixer {

import flash.events.Event;
import flash.events.IEventDispatcher;

import mx.containers.Grid;
import mx.containers.HBox;
import mx.containers.TitleWindow;

import mx.managers.PopUpManager;

import com.threerings.flex.CommandButton;
import com.threerings.flex.GridUtil;
import com.threerings.flex.PopUpUtil;

/**
 * Subclassable to allow editing a data field in a popup.
 */
public class PopupEditor extends TitleWindow
{
    public function PopupEditor (
        ctx :RemixContext, parent :DataEditor, entry :Object)
    {
        _parent = parent;
        this.title = entry.name;

        // configure the okbutton immediately
        _okBtn = new CommandButton(ctx.REMIX.get("b.ok"), close, true);

        // set up the grid with some standard bits
        var grid :Grid = new Grid();
        addChild(grid);
        GridUtil.addRow(grid, ctx.REMIX.get("l.name"), entry.name as String);
        var desc :String = entry.info as String;
        if (desc == null) {
            desc = ctx.REMIX.get("m.none");
        }
        GridUtil.addRow(grid, ctx.REMIX.get("l.desc"), desc);
        GridUtil.addRow(grid, ctx.REMIX.get("l.type"), entry.type as String);

        // add class-specific UI
        configureUI(ctx, entry, grid);

        // add the buttons to the bottom
        var buttonBar :HBox = new HBox();
        buttonBar.setStyle("horizontalAlign", "right");
        buttonBar.percentWidth = 100;
        buttonBar.addChild(new CommandButton(ctx.REMIX.get("b.cancel"), close, false));
        buttonBar.addChild(_okBtn);
        GridUtil.addRow(grid, buttonBar, [2, 1]);

        // and pop-up
        open();
    }

    protected function configureUI (ctx :RemixContext, entry :Object, grid :Grid) :void
    {
        // your subclass does stuff here
    }

    protected function getNewValue () :Object
    {
        return null; // your subclass does stuff here
    }

    protected function open () :void
    {
        PopUpManager.addPopUp(this, _parent, true);
        PopUpUtil.center(this);
    }

    protected function close (save :Boolean) :void
    {
        if (save) {
            _parent.updateValue(getNewValue());
        }

        PopUpManager.removePopUp(this);
    }

    protected var _parent :DataEditor;

    protected var _okBtn :CommandButton;
}
}

