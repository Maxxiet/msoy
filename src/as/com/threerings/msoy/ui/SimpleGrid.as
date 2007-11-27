//
// $Id$

package com.threerings.msoy.ui {

import flash.display.DisplayObject;

import mx.containers.Grid;
import mx.containers.GridItem;
import mx.containers.GridRow;
import mx.core.UIComponent;

import com.threerings.flex.GridUtil;

/**
 * Sticks a bunch of components into a grid. Doesn't support removal or other complexity. Add
 * support if you need it.
 */
public class SimpleGrid extends Grid
{
    public function SimpleGrid (columns :int)
    {
        _columns = columns;
    }

    public function get cellCount () :int
    {
        var count :int = 0;
        for (var ii :int = 0; ii < numChildren; ii++) {
            count += (getChildAt(ii) as GridRow).numChildren;
        }
        return count;
    }

    public function addCell (child :UIComponent, horizontalAlign :String = "center",
                             verticalAlign :String = "middle") :GridItem
    {
        if (_curRow == null) {
            _curRow = new GridRow();
            addChild(_curRow);
        }
        var item :GridItem = GridUtil.addToRow(_curRow, child);
        item.setStyle("horizontalAlign", horizontalAlign);
        item.setStyle("verticalAlign", verticalAlign);
        if (_curRow.numChildren == _columns) {
            _curRow = null;
        }
        return item;
    }

    public function getCellAt (index :int) :UIComponent
    {
        var row :GridRow = getChildAt(index / _columns) as GridRow;
        return ((row.getChildAt(index % _columns) as GridItem).getChildAt(0) as UIComponent);
    }

    public function clearCells () :void
    {
        while (numChildren > 0) {
            removeChildAt(0);
        }
        _curRow = null;
    }

    protected var _columns :int;
    protected var _curRow :GridRow;
}
}
