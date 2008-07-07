//
// $Id$

package client.games;

import java.util.List;
import java.util.Date;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import org.gwtwidgets.client.util.SimpleDateFormat;

import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.util.SimpleDataModel;

import com.threerings.msoy.game.data.all.Trophy;
import com.threerings.msoy.item.data.all.MediaDesc;

import client.util.MediaUtil;

/**
 * Displays the trophies 
 */
public class GameTrophyPanel extends PagedGrid<Trophy>
{
    public GameTrophyPanel (int gameId)
    {
        super(5, 2, NAV_ON_BOTTOM);
        _gameId = gameId;
        addStyleName("gameTrophyPanel");
        addStyleName("dottedGrid");
        add(new Label(CGames.msgs.gameTrophyLoading()));
        setCellAlignment(ALIGN_LEFT, ALIGN_TOP);
    }

    @Override // from UIObject
    public void setVisible (boolean visible)
    {
        super.setVisible(visible);
        if (!visible || _gameId == 0) {
            return;
        }

        CGames.gamesvc.loadGameTrophies(CGames.ident, _gameId, new AsyncCallback<List<Trophy>>() {
            public void onSuccess (List<Trophy> result) {
                setModel(new SimpleDataModel<Trophy>(result), 0);
            }
            public void onFailure (Throwable caught) {
                CGames.log("loadGameTrophies failed", caught);
                add(new Label(CGames.serverError(caught)));
            }
        });
        _gameId = 0; // note that we've asked for our data
    }

    @Override // from PagedGrid
    protected Widget createWidget (Trophy item)
    {
        return new TrophyDetail(item);
    }

    @Override // from PagedGrid
    protected String getEmptyMessage ()
    {
        return CGames.msgs.gameTrophyNoTrophies();
    }

    @Override // from PagedGrid
    protected boolean displayNavi (int items)
    {
        return true;
    }

    @Override // from PagedGrid
    protected boolean padToFullPage ()
    {
        return true;
    }

    protected class TrophyDetail extends FlexTable
    {
        public TrophyDetail (Trophy trophy) {
            setCellSpacing(0);
            setCellPadding(0);
            setStyleName("trophyDetail");

            if (trophy != null) {
                setWidget(0, 0, MediaUtil.createMediaView(
                              trophy.trophyMedia, MediaDesc.THUMBNAIL_SIZE));
                setText(0, 1, trophy.name);
                if (trophy.description == null) {
                    setText(1, 0, CGames.msgs.gameTrophySecret());
                    getFlexCellFormatter().setStyleName(1, 0, "Italic");
                } else {
                    setText(1, 0, trophy.description);
                }
                if (trophy.whenEarned != null) {
                    setText(2, 0, CGames.msgs.gameTrophyEarnedOn(
                                _pfmt.format(new Date(trophy.whenEarned.longValue()))));
                    getFlexCellFormatter().setStyleName(2, 0, "Earned");
                }
            }

            getFlexCellFormatter().setStyleName(0, 0, "Image");
            getFlexCellFormatter().setStyleName(0, 1, "Name");
            getFlexCellFormatter().setVerticalAlignment(1, 0, HasAlignment.ALIGN_TOP);
            getFlexCellFormatter().setRowSpan(0, 0, getRowCount());
        }
    }

    protected int _gameId;

    protected static SimpleDateFormat _pfmt = new SimpleDateFormat("MMM dd, yyyy");
}
