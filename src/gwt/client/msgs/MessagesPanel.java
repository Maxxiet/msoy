//
// $Id$

package client.msgs;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.InlineLabel;
import com.threerings.gwt.ui.PagedGrid;

import com.threerings.msoy.fora.data.ForumMessage;
import com.threerings.msoy.fora.data.ForumThread;

import client.images.msgs.MsgsImages;
import client.shell.Application;
import client.shell.Args;
import client.shell.Frame;
import client.shell.Page;
import client.util.MsoyCallback;
import client.util.MsoyUI;
import client.util.PromptPopup;

/**
 * Displays the messages in a particular thread.
 */
public class MessagesPanel extends PagedGrid
{
    public MessagesPanel (ThreadPanel parent, int scrollToId)
    {
        super(MESSAGES_PER_PAGE, 1, NAV_ON_BOTTOM);
        setCellAlignment(ALIGN_LEFT, ALIGN_TOP);
        addStyleName("dottedGrid");
        setWidth("100%");
        setHeight("100%");

        _parent = parent;
        _scrollToId = scrollToId;
    }

    public void refreshDisplay ()
    {
        displayPage(_page, true);
    }

    // @Override // from PagedGrid
    protected String getEmptyMessage ()
    {
        return CMsgs.mmsgs.noMessages();
    }

    // @Override // from PagedGrid
    protected boolean displayNavi (int items)
    {
        return true; // we always show our navigation for consistency
    }

    // @Override // from PagedGrid
    protected Widget createWidget (Object item)
    {
        ForumMessage msg = (ForumMessage)item;
        final ThreadMessagePanel panel = new ThreadMessagePanel(
            ((ForumModels.ThreadMessages)_model).getThread(), msg);
        if (msg.messageId == _scrollToId) {
            _scrollToId = 0;
            DeferredCommand.addCommand(new Command() {
                public void execute () {
                    Frame.ensureVisible(panel);
                }
            });
        }
        return panel;
    }

    // @Override // from PagedGrid
    protected void addCustomControls (FlexTable controls)
    {
        super.addCustomControls(controls);

        // add a button for starting a new message that will optionally be enabled later
        _postReply = new Button(CMsgs.mmsgs.postReply(), new ClickListener() {
            public void onClick (Widget sender) {
                _parent.postReply(null, false);
            }
        });
        _postReply.setEnabled(false);
        controls.setWidget(0, 0, _postReply);

        // add a button for editing this thread's flags
        _editFlags = new Button(CMsgs.mmsgs.editFlags(), new ClickListener() {
            public void onClick (Widget sender) {
                _parent.editFlags();
            }
        });
        _editFlags.setEnabled(false);
        controls.setWidget(0, 1, _editFlags);
    }

    // @Override // from PagedGrid
    protected void displayResults (int start, int count, List list)
    {
        ForumModels.ThreadMessages tmodel = (ForumModels.ThreadMessages)_model;
        _parent.gotThread(tmodel.getThread());
        _postReply.setEnabled(tmodel.canPostReply() && !tmodel.getThread().isLocked());
        _editFlags.setEnabled(tmodel.isManager());
        super.displayResults(start, count, list);
    }

    protected void replyPosted (ForumMessage message)
    {
        MsoyUI.info(CMsgs.mmsgs.msgReplyPosted());
        ((ForumModels.ThreadMessages)_model).appendItem(message);
        displayPage(_page, true);
    }

    protected Command deletePost (final ForumMessage message)
    {
        return new Command() {
            public void execute () {
                // TODO: if forum admin, make them send a mail to the poster explaining why their
                // post was deleted?
                CMsgs.forumsvc.deleteMessage(CMsgs.ident, message.messageId, new MsoyCallback() {
                    public void onSuccess (Object result) {
                        removeItem(message);
                        MsoyUI.info(CMsgs.mmsgs.msgPostDeleted());
                    }
                });
            }
        };
    }

    protected static Widget makeInfoImage (
        AbstractImagePrototype iproto, String tip, ClickListener onClick)
    {
        Widget image = MsoyUI.makeActionImage(iproto.createImage(), tip, onClick);
        image.addStyleName("ActionIcon");
        return image;
    }

    protected static InlineLabel makeInfoLabel (String text, ClickListener listener)
    {
        InlineLabel label = new InlineLabel(text, false, true, false);
        label.addClickListener(listener);
        label.addStyleName("Posted");
        label.addStyleName("actionLabel");
        return label;
    }

    protected class ThreadMessagePanel extends SimpleMessagePanel
    {
        public ThreadMessagePanel (ForumThread thread, ForumMessage message)
        {
            _thread = thread;
            setMessage(message);
        }

        public void setMessage (ForumMessage message)
        {
            _message = message;
            super.setMessage(message);
        }

        // @Override // from MessagePanel
        protected void addInfo (FlowPanel info)
        {
            super.addInfo(info);

            if (CMsgs.getMemberId() != 0 &&
                CMsgs.getMemberId() != _message.poster.name.getMemberId()) {
                String args = Args.compose("w", _message.poster.name.getMemberId());
                info.add(makeInfoLabel(CMsgs.mmsgs.inlineMail(),
                                       Application.createLinkListener(Page.MAIL, args)));
            }

            if (_postReply.isEnabled()) {
                info.add(makeInfoImage(_images.reply_post(),
                                                CMsgs.mmsgs.inlineReply(), new ClickListener() {
                    public void onClick (Widget sender) {
                        _parent.postReply(_message, false);
                    }
                }));
                info.add(makeInfoImage(_images.reply_post_quote(),
                                                CMsgs.mmsgs.inlineQReply(), new ClickListener() {
                    public void onClick (Widget sender) {
                        _parent.postReply(_message, true);
                    }
                }));
            }

            if (CMsgs.getMemberId() == _message.poster.name.getMemberId()) {
                info.add(makeInfoImage(_images.edit_post(),
                                                CMsgs.mmsgs.inlineEdit(), new ClickListener() {
                    public void onClick (Widget sender) {
                        _parent.editPost(_message, new MsoyCallback() {
                            public void onSuccess (Object result) {
                                setMessage((ForumMessage)result);
                            }
                        });
                    }
                }));
            }

            // TODO: if whirled manager, also allow forum moderation
            if (CMsgs.getMemberId() == _message.poster.name.getMemberId() || CMsgs.isAdmin()) {
                info.add(makeInfoImage(_images.delete_post(),
                                                CMsgs.mmsgs.inlineDelete(),
                                                new PromptPopup(CMsgs.mmsgs.confirmDelete(),
                                                                deletePost(_message))));
            }

            if (_message.issueId > 0) {
                ClickListener viewClick = Application.createLinkListener(
                    Page.WHIRLEDS, Args.compose("i", _message.issueId));
                info.add(makeInfoImage(_images.view_issue(),
                                                CMsgs.mmsgs.inlineIssue(), viewClick));

            } else if (CMsgs.isAdmin()) {
                ClickListener newClick = new ClickListener() {
                    public void onClick (Widget sender) {
                        _parent.newIssue(_message);
                    }
                };
                info.add(makeInfoImage(_images.new_issue(),
                                                CMsgs.mmsgs.inlineNewIssue(), newClick));
                info.add(makeInfoImage(_images.assign_issue(), CMsgs.mmsgs.inlineAssignIssue(),
                                       Application.createLinkListener(
                                           Page.WHIRLEDS, Args.compose(
                                               "assign", ""+_message.messageId, ""+_page))));
            }
        }

        // @Override // from MessagePanel
        protected String getIconPath ()
        {
            return "/images/msgs/" +
                ((_message.messageId > _thread.lastReadPostId) ? "unread" : "read") + ".png";
        }

        protected ForumThread _thread;
        protected ForumMessage _message;
    }

    /** The thread panel in which we're hosted. */
    protected ThreadPanel _parent;

    /** A message to scroll into view when we first receive our messages. */
    protected int _scrollToId;

    /** A button for posting a reply message. */
    protected Button _postReply;

    /** A button for editing this thread's flags. */
    protected Button _editFlags;

    /** Our action icon images. */
    protected static MsgsImages _images = (MsgsImages)GWT.create(MsgsImages.class);

    protected static final int MESSAGES_PER_PAGE = 10;
}
