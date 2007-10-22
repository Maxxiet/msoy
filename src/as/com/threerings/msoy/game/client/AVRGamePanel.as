//
// $Id: AVRGamePanel.as 5986 2007-10-02 13:46:12Z zell $

package com.threerings.msoy.game.client {

import flash.events.Event;
import flash.display.Loader;
import flash.display.Sprite;
import flash.display.LoaderInfo;

import mx.containers.Canvas;

import com.threerings.flash.MediaContainer;

import com.threerings.msoy.client.ControlBackend;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyController;
import com.threerings.msoy.client.WorldContext;

import com.threerings.msoy.game.client.AVRGameControlBackend;
import com.threerings.msoy.game.client.AVRGameController;
import com.threerings.msoy.game.client.GameContext;
import com.threerings.msoy.game.data.AVRGameObject;

import com.threerings.msoy.world.client.RoomView;

public class AVRGamePanel extends Canvas
{
    public static const log :Log = Log.getLog(AVRGamePanel);

    public function AVRGamePanel (
        mctx :WorldContext, gctx :GameContext, ctrl :AVRGameController)
    {
        super();

        _mctx = mctx;
        _gctx = gctx;
        _ctrl = ctrl;
    }

    public function init (gameObj :AVRGameObject) :void
    {
        _gameObj = gameObj;

        // create the backend
        _backend = new AVRGameControlBackend(_gctx, _ctrl, _gameObj);

        // create the container for the user media
        _mediaHolder = new MediaContainer(gameObj.gameMedia.getMediaPath());
        var loader :Loader = Loader(_mediaHolder.getMedia());

        // hook the backend up with the media
        _backend.init(loader);

        // set ourselves up properly once the media is loaded
        loader.contentLoaderInfo.addEventListener(Event.COMPLETE, mediaComplete);
    }

    public function tutorialEvent (eventName :String) :void
    {
        if (_backend) {
            _backend.tutorialEvent(eventName);
        }
    }

    public function shutdown () :void
    {
        _mctx.getMsoyController().setAVRGamePanel(null);
        // null gameObj for mediaComplete to find if it should run after us
        _gameObj = null;
    }

    protected function mediaComplete (event :Event) :void
    {
        var info :LoaderInfo = (event.target as LoaderInfo);
        info.removeEventListener(Event.COMPLETE, mediaComplete);

        if (_gameObj == null) {
            // we've already been shut down
            return;
        }

        _ctrl.gameIsReady();

        this.height = info.height + 2;

        this.rawChildren.addChild(_mediaHolder);

        _mctx.getMsoyController().setAVRGamePanel(this);
    }

    protected var _mctx :WorldContext;
    protected var _gctx :GameContext;
    protected var _ctrl :AVRGameController;
    protected var _mediaHolder :MediaContainer;
    protected var _gameObj :AVRGameObject;
    protected var _backend :AVRGameControlBackend;
}
}
