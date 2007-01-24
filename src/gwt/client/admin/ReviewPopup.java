package client.admin;

import java.util.Iterator;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.threerings.gwt.ui.InlineLabel;
import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.ItemDetail;
import com.threerings.msoy.item.web.MediaDesc;

import client.util.BorderedDialog;
import client.util.BorderedPopup;
import client.util.ClickCallback;
import client.util.MediaUtil;

/**
 * An interface for dealing with flagged items: mark them mature if they were flagged thus,
 * or delete them, or simply remove the flags.
 */
public class ReviewPopup extends BorderedDialog
{
    /**
     * Constructs a new {@link ReviewPopup}. 
     */
    public ReviewPopup (AdminContext ctx)
    {
        super(false, true);
        _ctx = ctx;
        
        _dock.setHorizontalAlignment(HasAlignment.ALIGN_CENTER);

        HorizontalPanel buttonRow = new HorizontalPanel();
        buttonRow.setSpacing(10);
        
        Button reloadButton = new Button(_ctx.msgs.reviewReload());
        reloadButton.addClickListener(new ClickListener() {
            public void onClick (Widget sender) {
                refresh();
            }
        });
        buttonRow.add(reloadButton);
        
        Button dismissButton = new Button(_ctx.msgs.reviewDismiss());
        dismissButton.addClickListener(new ClickListener() {
            public void onClick (Widget sender) {
                hide();
            }
        });
        buttonRow.add(dismissButton);
        _dock.add(buttonRow, DockPanel.SOUTH);
        
        _status = new Label();
        _dock.add(_status, DockPanel.SOUTH);
        _centerContent = new FlexTable();
        _dock.add(_centerContent, DockPanel.CENTER);

        refresh();
    }

    // @Override
    protected Widget createContents ()
    {
        _dock = new DockPanel();
        return _dock;
    }

    // clears the UI and repopuplates the list
    protected void refresh ()
    {
        _centerContent.clear();
        _ctx.itemsvc.getFlaggedItems(_ctx.creds, 10, new AsyncCallback() {
            public void onSuccess (Object result) {
                populateUI((List) result);
            }
            public void onFailure (Throwable caught) {
                _status.setText(_ctx.msgs.reviewErrFlaggedItems(caught.getMessage()));
            }
        });
    }

    // builds the UI from the given list
    protected void populateUI (List list)
    {
        _centerContent.clear();
        if (list.size() == 0) {
            _centerContent.setWidget(0, 0, new Label(_ctx.msgs.reviewNoItems()));
            return;
        }
        int row = 0;
        Iterator i = list.iterator();
        while (i.hasNext()) {
            ItemDetail itemDetail = (ItemDetail) i.next();
            
            // thumbnail to the left
            _centerContent.setWidget(row, 0, MediaUtil.createMediaView(
                itemDetail.item.getThumbnailMedia(), MediaDesc.THUMBNAIL_SIZE));

            _centerContent.setWidget(row, 1, new ItemBits(itemDetail));
            row ++;
        }
    }

    /**
     * A class one UI row representing one item.
     */
    protected class ItemBits extends VerticalPanel
    {
        /**
         * Build a new {@link ItemBits} object associated with the given {@link ItemDetail}.
         */
        public ItemBits (ItemDetail detail)
        {
            _item = detail.item;

            // first a horizontal line with item name & creator nam
            HorizontalPanel line = new HorizontalPanel();

            // the name popups an item inspector
            InlineLabel nameLabel = new InlineLabel(_item.name);
            nameLabel.addClickListener(new ClickListener() {
                public void onClick (Widget sender) {
                    new AdminItemPopup(_ctx, _item, ReviewPopup.this).show();
                }
            });
            line.add(nameLabel);
            line.add(new InlineLabel("  - " + detail.creator.toString()));
            add(line);

            add(new Label(_item.description));

            // then a row of action buttons
            line = new HorizontalPanel();

            // TODO: Let's nix 'delist' for a bit and see if we need it later.
//          if (item.ownerId == 0) {
//          Button button = new Button("Delist");
//          new ClickCallback(_ctx, button, _status) {
//          public boolean callService () {
//          _ctx.catalogsvc.listItem(_ctx.creds, item.getIdent(), false, this);
//          return true;
//          }
//          public boolean gotResult (Object result) {
//          if (result != null) {
//          _status.setText(_ctx.msgs.reviewDelisted());
//          return false; // don't reenable delist
//          }
//          _status.setText(_ctx.msgs.errListingNotFound());
//          return true;
//          }
//          };
//          line.add(button);
//          }

            // a button to mark someting as mature
            if (_item.isSet(Item.FLAG_FLAGGED_MATURE)) {
                markButton = new Button(_ctx.msgs.reviewMark());
                new ClickCallback(_ctx, markButton, _status) {
                    public boolean callService () {
                        _status.setText("");
                        if (_item == null) {
                            // should not happen, but let's be careful
                            return false;
                        }
                        _ctx.itemsvc.setFlags(_ctx.creds, _item.getIdent(), Item.FLAG_MATURE,
                            Item.FLAG_MATURE, this);
                        return true;
                    }
                    public boolean gotResult (Object result) {
                        _status.setText(_ctx.msgs.reviewMarked());
                        return false; // don't reenable button
                    }
                };
                line.add(markButton);
            }

            // a button to delete an item and possibly all its clones
            deleteButton = new Button(
                _item.ownerId != 0 ? _ctx.msgs.reviewDelete() : _ctx.msgs.reviewDeleteAll());
            deleteButton.addClickListener(new ClickListener() {
                public void onClick (Widget sender) {
                    _status.setText("");
                    if (_item == null) {
                        // should not happen, but let's be careful
                        return;
                    }
                    new DeleteDialog().show();
                }
            });
            line.add(deleteButton);

            // a button to signal we're done
            doneButton = new Button(_ctx.msgs.reviewDone());
            new ClickCallback(_ctx, doneButton, _status) {
                public boolean callService () {
                    _status.setText("");
                    if (_item == null) {
                        refresh();
                        return false;
                    }
                    _ctx.itemsvc.setFlags(
                        _ctx.creds, _item.getIdent(),
                        (byte) (Item.FLAG_FLAGGED_COPYRIGHT | Item.FLAG_FLAGGED_MATURE),
                        (byte) 0, this);
                    return true;
                }
                public boolean gotResult (Object result) {
                    // the flags are set: refresh the UI
                    refresh();
                    // keep the button disabled until the UI refreshes
                    return false;
                }
            };
            line.add(doneButton);
            add(line);
        }

        /**
         * Handle the deletion message and prompt.
         */
        protected class DeleteDialog extends BorderedPopup
            implements KeyboardListener
        {
            public DeleteDialog ()
            {
                VerticalPanel content = new VerticalPanel();
                content.setHorizontalAlignment(HasAlignment.ALIGN_CENTER);

                content.add(new Label(_ctx.msgs.reviewDeletionPrompt()));

                _area = new TextArea();
                _area.setCharacterWidth(60);
                _area.setVisibleLines(4);
                _area.addKeyboardListener(this);
                content.add(_area);

                _feedback = new Label();
                content.add(_feedback);

                _yesButton = new Button(_ctx.msgs.reviewDeletionDo());
                _yesButton.setEnabled(false);
                final Button noButton = new Button(_ctx.msgs.reviewDeletionDont());
                ClickListener listener = new ClickListener () {
                    public void onClick (Widget sender) {
                        if (sender == _yesButton) {
                            doDelete();
                        }
                        hide();
                    }
                };
                _yesButton.addClickListener(listener);
                noButton.addClickListener(listener);
                HorizontalPanel buttons = new HorizontalPanel();
                buttons.setSpacing(10);
                buttons.add(_yesButton);
                buttons.add(noButton);
                content.add(buttons);

                setWidget(content);
                show();
            }

            public void onKeyDown (Widget sender, char keyCode, int modifiers) { /* empty*/ }
            public void onKeyPress (Widget sender, char keyCode, int modifiers) { /* empty */ }
            public void onKeyUp (Widget sender, char keyCode, int modifiers)
            {
                _yesButton.setEnabled(_area.getText().trim().length() > 0);
            }

            protected void doDelete ()
            {
                if (!_yesButton.isEnabled()) {
                    // you just never know
                    return;
                }
                _ctx.itemsvc.deleteItemAdmin(
                   _ctx.creds, _item.getIdent(), _ctx.msgs.reviewDeletionMailHeader(),
                   _ctx.msgs.reviewDeletionMailMessage(_item.name, _area.getText().trim()),
                   new AsyncCallback() {
                       public void onSuccess (Object result) {
                           _status.setText(_ctx.msgs.reviewDeletionSuccess(result.toString()));
                           if (markButton != null) {
                               markButton.setEnabled(false);
                           }
                           deleteButton.setEnabled(false);
                           _item = null;
                           hide();
                       }
                       public void onFailure (Throwable caught) {
                           _feedback.setText(
                               _ctx.msgs.reviewErrDeletionFailed(caught.getMessage()));
                           if (markButton != null) {
                               markButton.setEnabled(true);
                           }
                           deleteButton.setEnabled(true);
                           hide();
                       }
                   });
            }
            
            protected TextArea _area;
            protected Label _feedback;
            protected Button _yesButton;
        }

        protected Button markButton, deleteButton, doneButton;
        protected Item _item;
    }

    protected AdminContext _ctx;
    protected DockPanel _dock;
    protected FlexTable _centerContent;
    protected Label _status;
}
