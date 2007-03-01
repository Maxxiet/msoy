//
// $Id$

package com.threerings.msoy.client {

import flash.display.DisplayObject;
import flash.display.Stage;

import flash.external.ExternalInterface;
import flash.system.Security;

import mx.resources.ResourceBundle;

import com.adobe.crypto.MD5;

import com.threerings.util.Name;
import com.threerings.util.ResultAdapter;
import com.threerings.util.StringUtil;

import com.threerings.presents.client.Client;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.DSet;

import com.threerings.presents.dobj.DObjectManager;

import com.threerings.presents.net.BootstrapData;

import com.threerings.presents.data.TimeBaseMarshaller;
import com.threerings.crowd.data.BodyMarshaller;
import com.threerings.crowd.data.LocationMarshaller;
import com.threerings.crowd.chat.data.ChatMarshaller;

import com.threerings.msoy.data.MemberInfo;
import com.threerings.msoy.data.MemberMarshaller;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyAuthResponseData;
import com.threerings.msoy.data.MsoyBootstrapData;
import com.threerings.msoy.data.MsoyCredentials;
import com.threerings.msoy.data.SceneBookmarkEntry;

import com.threerings.msoy.web.data.FriendEntry;

/**
 * A client shared by both our virtual world and header incarnations.
 */
public /*abstract*/ class BaseClient extends Client
{
    public static const log :Log = Log.getLog(BaseClient);

    /**
     * Notifies our JavaScript shell that our flow, gold, etc. levels have updated.
     */
    public static function levelsUpdated () :void
    {
        try {
            if (ExternalInterface.available) {
                ExternalInterface.call("levelsUpdated");
            }
        } catch (err :Error) {
            log.warning("ExternalInterface.call('levelsUpdated') failed: " + err);
        }
    }

    public function BaseClient (stage :Stage)
    {
        super(createStartupCreds(stage), stage);

        _ctx = createContext();
        LoggingTargets.configureLogging(_ctx);

        // wire up our JavaScript bridge functions
        try {
            if (ExternalInterface.available) {
                configureExternalFunctions();
            }
        } catch (err :Error) {
            // nada: ExternalInterface isn't there. Oh well!
            log.info("Unable to configure external functions.");
        }

        // allow connecting to the game server
        Security.loadPolicyFile("http://" + DeploymentConfig.serverHost + "/crossdomain.xml");
        // and the media server if it differs from the game server
        if (DeploymentConfig.mediaURL.indexOf(DeploymentConfig.serverHost) == -1) {
            Security.loadPolicyFile(DeploymentConfig.mediaURL + "crossdomain.xml");
        }

        // configure our server and port info and logon
        setServer(DeploymentConfig.serverHost, DeploymentConfig.serverPorts);
    }

    public function fuckingCompiler () :void
    {
        var i :int = TimeBaseMarshaller.GET_TIME_OID;
        i = LocationMarshaller.LEAVE_PLACE;
        i = BodyMarshaller.SET_IDLE;
        i = ChatMarshaller.AWAY;

        var c :Class;
        c = MsoyBootstrapData;
        c = MemberObject;
        c = MemberInfo;
        c = MsoyAuthResponseData;
        c = MemberMarshaller;
        c = SceneBookmarkEntry;

        [ResourceBundle("global")]
        var rb :ResourceBundle;
    }

    // from Client
    override public function gotBootstrap (data :BootstrapData, omgr :DObjectManager) :void
    {
        super.gotBootstrap(data, omgr);

        // save any machineIdent or sessionToken from the server.
        var rdata :MsoyAuthResponseData = (getAuthResponseData() as MsoyAuthResponseData);
        if (rdata.ident != null) {
            Prefs.setMachineIdent(rdata.ident);
        }
        if (rdata.sessionToken != null) {
            Prefs.setSessionToken(rdata.sessionToken);
        }

        if (rdata.sessionToken != null) {
            try {
                if (ExternalInterface.available) {
                    ExternalInterface.call("flashDidLogon", "Foo", 1, rdata.sessionToken);
                }
            } catch (err :Error) {
                log.warning("Unable to inform javascript about login: " + err);
            }
        }

        log.info("Client logged on [built=" + DeploymentConfig.buildTime + "].");
    }

    // from Client
    override public function gotClientObject (clobj :ClientObject) :void
    {
        super.gotClientObject(clobj);

        // set up our logging targets
        LoggingTargets.configureLogging(_ctx);

        // listen for flow and gold updates
        _user = (clobj as MemberObject);
        _user.addListener(new LevelUpdater());
        // configure our levels to start
        levelsUpdated();

        // possibly ensure our local storage capacity
        if (!_user.isGuest()) {
            Prefs.config.ensureCapacity(
                102400, new ResultAdapter(null, function (cause :Error) :void {
                log.warning("User denied request to increase local storage capacity.");
            }));
        }
    }

    /**
     * Configure any external functions that we wish to expose to JavaScript.
     */
    protected function configureExternalFunctions () :void
    {
        ExternalInterface.addCallback("onUnload", externalOnUnload);
        ExternalInterface.addCallback("getFriends", externalGetFriends);
        ExternalInterface.addCallback("getLevels", externalGetLevels);
    }

    /**
     * Exposed to JavaScript so that it may notify us when we're leaving the page.
     */
    protected function externalOnUnload () :void
    {
        log.info("Client unloaded. Logging off.");
        logoff(false);
    }

    /**
     * Provides this player's friends list to the GWT client.
     */
    protected function externalGetFriends () :Array
    {
        if (_user == null) {
            log.info("externalGetFriends() without MemberObject.");
            return new Array();
        }

        // we have to convert everything to an array of primitives, so we convert to an array of
        // String, Number, Boolean (repeat) (an array of arrays doesn't work; yay!)
        var friends :Array = _user.getSortedEstablishedFriends();
        var fdata :Array = new Array();
        friends.forEach(function (entry :FriendEntry, index :int, array :Array) :void {
            fdata.push(entry.name.toString());
            fdata.push(entry.name.getMemberId());
            fdata.push(entry.online);
        });
        return fdata;
    }

    /**
     * Provides this player's flow, gold and level levels to the GWT client.
     */
    protected function externalGetLevels () :Array
    {
        var levels :Array = new Array(3);
        if (_user == null) {
            log.info("externalGetLevels() without MemberObject.");
            levels[0] = 0;
            levels[1] = 0;
            levels[2] = 0;
        } else {
            levels[0] = _user.flow;
            levels[1] = 0; // _user.gold;
            levels[2] = 1; // _user.level;
        }
        return levels;
    }

    /**
     * Creates the context we'll use with this client.
     */
    protected function createContext () :BaseContext
    {
        return new BaseContext(this);
    }

    /**
     * Create the credentials that will be used to log us on
     */
    protected static function createStartupCreds (stage :Stage, token :String = null)
        :MsoyCredentials
    {
        var params :Object = stage.loaderInfo.parameters;
        var creds :MsoyCredentials;
        if (DeploymentConfig.devClient && (params["pass"] != null) && (params["user"] != null)) {
            creds = new MsoyCredentials(new Name(String(params["user"])),
                MD5.hash(String(params["pass"])));

        } else {
            creds = new MsoyCredentials(null, null);
        }
        creds.ident = Prefs.getMachineIdent();
        if (null == params["guest"]) {
            creds.sessionToken = (token == null) ? params["token"] : token;
        }
        return creds;
    }

    protected var _ctx :BaseContext;
    protected var _user :MemberObject;
}
}

import flash.external.ExternalInterface;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;

import com.threerings.msoy.client.BaseClient;
import com.threerings.msoy.data.MemberObject;

class LevelUpdater implements AttributeChangeListener
{
    public function attributeChanged (event :AttributeChangedEvent) :void {
        if (/* event.getName() == MemberObject.GOLD || */ event.getName() == MemberObject.FLOW) {
            BaseClient.levelsUpdated();
        }
    }
}
