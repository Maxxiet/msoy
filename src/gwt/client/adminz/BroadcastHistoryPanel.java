package client.adminz;

import java.util.ArrayList;
import java.util.List;

import client.ui.MsoyUI;
import client.util.Link;
import client.util.PagedServiceDataModel;
import client.util.ServiceUtil;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.PagedTable;

import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.admin.gwt.AdminService;
import com.threerings.msoy.admin.gwt.AdminServiceAsync;
import com.threerings.msoy.admin.gwt.AdminService.BroadcastHistoryResult;

import com.threerings.msoy.money.gwt.BroadcastHistory;

/**
 * Admin/support panel for viewing broadcast history.
 */
public class BroadcastHistoryPanel extends VerticalPanel
{
    /**
     * Creates a new panel.
     */
    public BroadcastHistoryPanel ()
    {
        setStyleName("BroadcastHistory");
        setWidth("100%");
        PagedTable<BroadcastHistory> table = new PagedTable<BroadcastHistory>(20) {
            @Override protected List<Widget> createHeader () {
                List<Widget> labels = new ArrayList<Widget>();
                labels.add(MsoyUI.createLabel(_msgs.broadcastTimeSent(), "time"));
                labels.add(MsoyUI.createLabel(_msgs.broadcastMember(), "member"));
                labels.add(MsoyUI.createLabel(_msgs.broadcastBarsPaid(), "bars"));
                labels.add(MsoyUI.createLabel(_msgs.broadcastMessage(), "message"));
                return labels;
            }
            @Override protected List<Widget> createRow (BroadcastHistory item) {
                MemberName memberName = _result.memberNames.get(item.memberId);
                if (memberName == null) {
                    memberName = new MemberName(_msgs.broadcastMemberDeleted(), item.memberId);
                }
                List<Widget> fields = new ArrayList<Widget>();
                fields.add(MsoyUI.createLabel(MsoyUI.formatDateTime(item.timeSent), "time"));
                fields.add(Link.memberView(memberName));
                fields.add(MsoyUI.createLabel(String.valueOf(item.barsPaid), "bars"));
                fields.add(MsoyUI.createLabel(item.message, "message"));
                return fields;
            }
            @Override protected String getEmptyMessage () {
                return _msgs.broadcastNoneFound();
            }
        };
        table.setWidth("100%");
        table.addStyleName("table");
        add(table);

        table.setModel(new PagedServiceDataModel<BroadcastHistory, BroadcastHistoryResult>() {
            @Override protected void callFetchService (int start, int count, boolean needCount,
                AsyncCallback<BroadcastHistoryResult> callback) {
                _adminsvc.getBroadcastHistory(start, count, needCount, callback);
            }
            @Override protected void setCurrentResult (BroadcastHistoryResult result) {
                _result = result;
            }
        }, 0);
    }

    /** The most recent result, for looking up member names. */
    protected BroadcastHistoryResult _result;

    protected static final AdminMessages _msgs = GWT.create(AdminMessages.class);
    protected static final AdminServiceAsync _adminsvc = (AdminServiceAsync)
        ServiceUtil.bind(GWT.create(AdminService.class), AdminService.ENTRY_POINT);
}