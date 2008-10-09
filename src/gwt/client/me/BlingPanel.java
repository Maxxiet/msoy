//
// $Id$

package client.me;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.KeyboardListenerAdapter;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.InlineLabel;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.msoy.money.data.all.BlingExchangeResult;
import com.threerings.msoy.money.data.all.BlingInfo;
import com.threerings.msoy.money.data.all.CashOutBillingInfo;
import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.gwt.MoneyService;
import com.threerings.msoy.money.gwt.MoneyServiceAsync;

import client.shell.CShell;
import client.shell.ShellMessages;
import client.ui.MsoyUI;
import client.ui.NumberTextBox;
import client.ui.TongueBox;
import client.util.ServiceUtil;
import client.util.StringUtil;
import client.util.events.StatusChangeEvent;

public class BlingPanel extends FlowPanel
{
    public BlingPanel (final MoneyTransactionDataModel model)
    {
        setStyleName("bling");
        
        _model = model;
        init();
    }
    
    protected void init ()
    {
        int row = 0;

        SmartTable balance = new SmartTable(0, 10);
        balance.setText(0, 0, _msgs.blingBalance(), 1, "rightLabel");
        balance.setWidget(0, 1, _blingBalance = new Label());
        balance.setText(1, 0, _msgs.blingWorth(), 1, "rightLabel");
        balance.setWidget(1, 1, _blingWorth = new Label());

        add(new TongueBox(_msgs.blingHeader(), balance));

        SmartTable exchange = new SmartTable(0, 10);
        exchange.setText(0, 0, _msgs.exchangeBlingDescription(), 3, null);
        exchange.setText(1, 0, _msgs.exchangeAmount(), 1, "rightLabel");
        exchange.setWidget(1, 1, _exchangeBox = new NumberTextBox(true));
        exchange.setWidget(1, 2, _exchangeBtn = new Button(_msgs.exchangeButton(), new ClickListener() {
            public void onClick (Widget sender) {
                doExchange(_model.memberId);
            }
        }));

        add(new TongueBox(_msgs.exchangeBlingForBars(), exchange));

        _cashOutPanel = new SimplePanel();
        add(new TongueBox(_msgs.blingCashOutHeader(), _cashOutPanel));

        _model.addBlingCallback(new AsyncCallback<BlingInfo>() {
            public void onFailure (Throwable caught) {
                // Ignore
            }
            public void onSuccess (BlingInfo result) {
                update(result);
            }
        });
    }
    
    protected void update (final BlingInfo result)
    {
        _blingBalance.setText(Currency.BLING.format(result.bling));
        _blingWorth.setText(formatUSD((int)(result.worthPerBling * result.bling)));
        if (result.cashOut != null) {
            HorizontalPanel row = new HorizontalPanel();
            row.add(MsoyUI.createLabel(_msgs.cashedOutBling(
                Currency.BLING.format(result.cashOut.blingAmount),
                formatUSD(result.cashOut.blingWorth)), "Success"));
            row.add(new Button(_cmsgs.cancel(), new ClickListener() {
                public void onClick (final Widget sender) {
                    _moneysvc.cancelCashOut(_model.memberId, "m.user_cancelled", new AsyncCallback<Void>() {
                        public void onFailure (Throwable cause) {
                            MsoyUI.errorNear(CShell.serverError(cause), sender);
                        }
                        public void onSuccess (Void v) {
                            MsoyUI.info(_msgs.cancelCashOutSuccess());
                            // Show the cash out form
                            result.cashOut = null;
                            update(result);
                        }
                    });
                }
            }));
            _cashOutPanel.setWidget(row);
        } else {
            if (result.bling >= result.minCashOutBling) {
                _cashOutPanel.setWidget(new CashOutForm(result.worthPerBling));
            } else {
                _cashOutPanel.setWidget(MsoyUI.createLabel(_msgs.cashOutBelowMinimum(
                    Float.toString(result.minCashOutBling/100.0f)), "Error"));
            }
            _cashedOutBling.setText("");
        }
    }
    
    protected class CashOutForm extends SmartTable
    {
        public CashOutForm (final float worthPerBling)
        {
            setCellSpacing(10);
            
            int row = 0;
            setText(row++, 0, _msgs.blingCashOutDescription(), 3, null);
            setText(row, 0, _msgs.blingCashOutAmount(), 1, "rightLabel");
            FlowPanel panel = new FlowPanel();
            final Label worthLabel = new InlineLabel();
            panel.add(_cashOutBox = new NumberTextBox(true));
            panel.add(worthLabel);
            setWidget(row++, 1, panel, 2, null);
            _cashOutBox.addKeyboardListener(new KeyboardListenerAdapter() {
                public void onKeyUp (Widget sender, char keyCode, int modifiers) {
                    worthLabel.setText(_msgs.cashOutAmountWorth(formatUSD(
                        (int)(_cashOutBox.getValue().floatValue() * 100.0f * worthPerBling))));
                }
            });
            setText(row, 0, _msgs.fieldTemplate(_msgs.cashOutPassword()), 1, "rightLabel");
            setWidget(row++, 1, _passwordBox = new PasswordTextBox());
            setText(row, 0, _msgs.fieldTemplate(_msgs.cashOutPayPalEmail()), 1, "rightLabel");
            setWidget(row++, 1, _paypalEmailBox = new TextBox());
            _paypalEmailBox.setText(CShell.creds.accountName);
            setText(row, 0, _msgs.fieldTemplate(_msgs.cashOutConfirmPayPalEmail()), 1, "rightLabel");
            setWidget(row++, 1, _paypalEmailConfirmBox = new TextBox());
            setText(row, 0, _msgs.fieldTemplate(_msgs.cashOutFirstName()), 1, "rightLabel");
            setWidget(row++, 1, _firstNameBox = new TextBox());
            setText(row, 0, _msgs.fieldTemplate(_msgs.cashOutLastName()), 1, "rightLabel");
            setWidget(row++, 1, _lastNameBox = new TextBox());
            setText(row, 0, _msgs.fieldTemplate(_msgs.cashOutPhoneNumber()), 1, "rightLabel");
            setWidget(row++, 1, _phoneNumberBox = new TextBox());
            setText(row, 0, _msgs.fieldTemplate(_msgs.cashOutStreetAddress()), 1, "rightLabel");
            setWidget(row++, 1, _streetAddressBox = new TextBox());
            setText(row, 0, _msgs.fieldTemplate(_msgs.cashOutCity()), 1, "rightLabel");
            setWidget(row++, 1, _cityBox = new TextBox());
            setText(row, 0, _msgs.fieldTemplate(_msgs.cashOutState()), 1, "rightLabel");
            setWidget(row++, 1, _stateBox = new TextBox());
            setText(row, 0, _msgs.fieldTemplate(_msgs.cashOutPostalCode()), 1, "rightLabel");
            setWidget(row++, 1, _postalCodeBox = new TextBox());
            setText(row, 0, _msgs.fieldTemplate(_msgs.cashOutCountry()), 1, "rightLabel");
            setWidget(row++, 1, _countryBox = new TextBox());
            setWidget(row++, 2, _cashOutBtn =
                      new Button(_msgs.blingCashOutButton(), new ClickListener() {
                public void onClick (Widget sender) {
                    doCashOut(_model.memberId);
                }
            }));
        }
        
        protected void doCashOut (int memberId)
        {
            // Validate the data
            int blingAmount = getValidAmount(_cashOutBox, _msgs.blingInvalidAmount());
            if (blingAmount == 0) {
                return;
            }
            if (!requireField(_passwordBox, _msgs.cashOutPassword()) ||
                !requireField(_firstNameBox, _msgs.cashOutFirstName()) ||
                !requireField(_lastNameBox, _msgs.cashOutLastName()) ||
                !requireField(_paypalEmailBox, _msgs.cashOutPayPalEmail()) ||
                !requireField(_phoneNumberBox, _msgs.cashOutPhoneNumber()) ||
                !requireField(_streetAddressBox, _msgs.cashOutStreetAddress()) ||
                !requireField(_cityBox, _msgs.cashOutCity()) ||
                !requireField(_stateBox, _msgs.cashOutState()) ||
                !requireField(_postalCodeBox, _msgs.cashOutPostalCode()) ||
                !requireField(_countryBox, _msgs.cashOutCountry())) {
                return;
            }
            if (!_paypalEmailBox.getText().equals(_paypalEmailConfirmBox.getText())) {
                MsoyUI.errorNear(_msgs.cashOutEmailsDontMatch(), _paypalEmailConfirmBox);
                return;
            }
            
            // Ensure the amount is valid.
            _cashOutBtn.setEnabled(false);
            try {
                String password = CShell.frame.md5hex(_passwordBox.getText());
                _moneysvc.requestCashOutBling(memberId, blingAmount, password, 
                    new CashOutBillingInfo(_firstNameBox.getText(), _lastNameBox.getText(), 
                    _paypalEmailBox.getText(), _phoneNumberBox.getText(), _streetAddressBox.getText(), 
                    _cityBox.getText(), _stateBox.getText(), _postalCodeBox.getText(), 
                    _countryBox.getText()), new AsyncCallback<BlingInfo>() {
                    public void onFailure (Throwable cause) {
                        MsoyUI.error(CShell.serverError(cause));
                    }
                    public void onSuccess (BlingInfo result) {
                        MsoyUI.info(_msgs.cashOutRequestSuccessful());
                        _cashOutBox.setText("");
                        update(result);
                    }
                });
            } finally {
                _cashOutBtn.setEnabled(true);
            }
        }
        
        protected NumberTextBox _cashOutBox;
        protected PasswordTextBox _passwordBox;
        protected TextBox _firstNameBox;
        protected TextBox _lastNameBox;
        protected TextBox _paypalEmailBox;
        protected TextBox _paypalEmailConfirmBox;
        protected TextBox _phoneNumberBox;
        protected TextBox _streetAddressBox;
        protected TextBox _cityBox;
        protected TextBox _stateBox;
        protected TextBox _postalCodeBox;
        protected TextBox _countryBox;
        
        protected Button _cashOutBtn;
    }
    
    protected void doExchange (int memberId)
    {
        // Validate the data
        int blingAmount = getValidAmount(_exchangeBox, _msgs.blingInvalidAmount());
        if (blingAmount == 0) {
            return;
        }
        
        // Ensure the amount is valid.
        _exchangeBtn.setEnabled(false);
        _moneysvc.exchangeBlingForBars(memberId, blingAmount,
            new AsyncCallback<BlingExchangeResult>() {
            public void onFailure (Throwable cause) {
                _exchangeBtn.setEnabled(true);
                MsoyUI.errorNear(CShell.serverError(cause), _exchangeBox);
            }
            public void onSuccess (BlingExchangeResult result) {
                _exchangeBtn.setEnabled(true);
                MsoyUI.info(_msgs.blingExchangeSuccessful());
                _exchangeBox.setText("");
                update(result.blingInfo);
                CShell.frame.dispatchEvent(new StatusChangeEvent(StatusChangeEvent.BARS, 
                    result.barBalance, 0));
            }
        });
    }
    
    protected boolean requireField (TextBox box, String fieldName)
    {
        if (StringUtil.isBlank(box.getText())) {
            MsoyUI.errorNear(_msgs.fieldRequired(fieldName), box);
            return false;
        }
        return true;
    }
    
    protected int getValidAmount (NumberTextBox box, String invalidMessage)
    {
        final int blingAmount = box.getValue().intValue();
        if (blingAmount < 1) {
            MsoyUI.errorNear(invalidMessage, box);
            return 0;
        }
        return blingAmount;
    }
    
    /**
     * Converts the amount of pennies into a string to display to the user as a valid currency.
     * Note: there are some other utilities around to do this, but they're either in a different
     * project (and there's some concern about exposing them directly), or they don't properly
     * take into account floating-point round off errors.  This may get replaced or expanded
     * later on.
     */
    protected static String formatUSD (int pennies)
    {
        int dollars = pennies / 100;
        int cents = pennies % 100;
        return "USD $" + NumberFormat.getDecimalFormat().format(dollars) + '.' +
            (cents < 10 ? '0' : "") + cents;
    }

    protected Label _blingBalance;
    protected Label _blingWorth;
    protected Label _cashedOutBling;
    
    protected NumberTextBox _exchangeBox;
    protected Button _exchangeBtn;
    
    protected SimplePanel _cashOutPanel;

    protected MoneyTransactionDataModel _model;
    
    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
    protected static final MeMessages _msgs = GWT.create(MeMessages.class);
    protected static final MoneyServiceAsync _moneysvc = (MoneyServiceAsync)
        ServiceUtil.bind(GWT.create(MoneyService.class), MoneyService.ENTRY_POINT);
}
