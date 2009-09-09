//
// $Id$

package client.frame;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;

import com.threerings.gwt.ui.AbsoluteCSSPanel;
import com.threerings.msoy.web.gwt.SessionData;

import client.shell.Session;
import client.shell.ShellMessages;
import client.ui.MsoyUI;

import client.util.events.FlashEventListener;
import client.util.events.FlashEvents;
import client.util.events.StatusChangeEvent;
import client.util.events.StatusChangeListener;
import client.util.events.TrophyEvent;

/**
 * Status panel for the Facebook application. Shows name, current level, number of trophies and
 * maybe at some point the daily bonus progress.
 */
public class FacebookStatusPanel extends AbsoluteCSSPanel
{
    public FacebookStatusPanel ()
    {
        super("fbstatus");
        add(MsoyUI.createAbsoluteCSSPanel("Top",
            _name = MsoyUI.createLabel("", "Name"),
            _level = MsoyUI.createLabel("", "Level"),
            MsoyUI.createFlowPanel("TrophyIcon"),
            _trophies = MsoyUI.createLabel("", "Trophies")));
        add(_levelProgressBar = new ProgressBar("LevelProgress", _msgs.fbStatusNextLevel()));

        Session.addObserver(_observer = new Session.Observer() {
            @Override public void didLogon (SessionData data) {
                setData(new Data(data));
            }
            @Override public void didLogoff () {
            }
        });

        _listeners.add(new StatusChangeListener() {
            public void statusChanged (StatusChangeEvent event) {
                switch(event.getType()) {
                case StatusChangeEvent.LEVEL:
                    // this is a bit hacky, but we need to know whether the didLogon handler
                    // in StatusPanel is the sender. It always uses 0 as the old value. If we don't
                    // perform this check we go into an infinite loop, continuously validating the
                    // session
                    // TODO: better alternative, perhaps just request the SessionData without
                    // dispatching a didLogon... or maybe include the information with a new
                    // LevelChangedEvent
                    if (event.getOldValue() != 0) {
                        // revalidate since we need the last and next coin values
                        //CShell.log("Level change from flash");
                        //Session.validate();
                    }
                    break;

                case StatusChangeEvent.COINS:
                    // hacky... see above
                    if (event.getOldValue() != 0 && _data != null) {
                        _data.currCoins += event.getValue() - event.getOldValue();
                        update();
                    }
                    break;
                }
            }
        });

        _listeners.add(new TrophyEvent.Listener() {
            @Override public void trophyEarned (TrophyEvent event) {
                if (_data != null) {
                    _data.trophies++;
                    update();
                }
            }
        });

        for (FlashEventListener listener : _listeners) {
            FlashEvents.addListener(listener);
        }

        if (_data != null) {
            update();
        }
    }

    @Override
    protected void onUnload ()
    {
        super.onUnload();
        for (FlashEventListener listener : _listeners) {
            FlashEvents.removeListener(listener);
        }
        if (_observer != null) {
            Session.removeObserver(_observer);
        }
        _listeners.clear();
        _observer = null;
    }

    protected void setData (Data data)
    {
        _data = data;
        update();
    }

    protected void update ()
    {
        _name.setText(_data.name);
        _level.setText(_msgs.fbstatusLevel(String.valueOf(_data.level)));
        _trophies.setText("" + _data.trophies);
        if (_data.nextCoins != 0) {
            _levelProgressBar.set(_data.lastCoins, _data.nextCoins, _data.currCoins);
        } else {
            _levelProgressBar.setVisible(false);
        }
    }

    protected static class ProgressBar extends AbsoluteCSSPanel
    {
        public ProgressBar (String style, String label)
        {
            super("fbprogressBar");
            addStyleName(style);
            add(_meter = MsoyUI.createFlowPanel("Meter"));
            add(MsoyUI.createLabel(label, "Label"));
            add(_detail = MsoyUI.createLabel("", "Detail"));
        }

        public void set (int min, int max, int current)
        {
            _min = min;
            _max = max;
            _current = current;
            update();
        }

        protected void update ()
        {
            int range = _max - _min;
            int progress = _current - _min;
            float percent = (float)progress / range * 100;
            _detail.setText(_msgs.fbStatusProgress(""+progress, ""+range));
            DOM.setStyleAttribute(_meter.getElement(), "width", (int)percent + "%");
        }

        protected Label _detail;
        protected FlowPanel _meter;
        protected int _min, _max, _current;
    }

    protected class Data
    {
        public String name;
        public int level;
        public int trophies;
        public int lastCoins;
        public int nextCoins;
        public int currCoins;

        public Data (SessionData sessionData)
        {
            name = sessionData.creds.name.toString();
            level = sessionData.level;
            if (sessionData.extra != null) {
                trophies = sessionData.extra.trophyCount;
                lastCoins = sessionData.extra.levelFlow;
                nextCoins = sessionData.extra.nextLevelFlow;
                currCoins = sessionData.extra.accumFlow;
            }
        }
    }

    protected Label _name, _level, _trophies;
    protected ProgressBar _levelProgressBar;
    protected Session.Observer _observer;
    protected List<FlashEventListener> _listeners = new ArrayList<FlashEventListener>();
    // bah, we have to use a static here because the title bar keeps getting re-created but
    // didLogon is only called the first time
    // TODO: some day sort this out 
    protected static Data _data;
    protected static final ShellMessages _msgs = GWT.create(ShellMessages.class); 
}