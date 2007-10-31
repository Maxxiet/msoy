//
// $Id$

package com.threerings.msoy.web.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.item.data.all.MediaDesc;

/** 
 * Contains the data that we need for the My Whirled or Whirledwide views.
 */
public class MyWhirledData
    implements IsSerializable
{
    /** 
     * The list of places for this view.
     *
     * @gwt.typeArgs <com.threerings.msoy.web.data.SceneCard>
     */
    public List places = new ArrayList();

    /** 
     * The list of games for this view.
     * 
     * @gwt.typeArgs <com.threerings.msoy.web.data.SceneCard>
     */
    public List games = new ArrayList();

    /** 
     * The list of people for this view.
     *
     * @gwt.typeArgs <com.threerings.msoy.web.data.MemberCard>
     */
    public List people = new ArrayList();

    /** 
     * Our recent feed messages.
     *
     * @gwt.typeArgs <com.threerings.msoy.person.data.FeedMessage>
     */
    public List feed = new ArrayList();

    /** 
     * This person's profile pic.
     */
    public MediaDesc photo;

    /**
     * The list of rooms owned by this person.
     *
     * @gwt.typeArgs <java.lang.Integer,java.lang.String>
     */
    public Map ownedRooms;

    /**
     * The list of active group chats this player is a member of.
     *
     * @gwt.typeArgs <java.lang.Integer,java.lang.String>
     */
    public Map chats;

    /**
     * The current total whirled population, as of the last Snapshot.
     */
    public int whirledPopulation;
}
