//
// $Id$

package com.threerings.msoy.game.gwt;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.gwt.MemberItemInfo;

/**
 * Contains information displayed on a game's detail page.
 */
public class GameDetail
    implements IsSerializable
{
    /** The maximum allowed length for game instructions. */
    public static final int MAX_INSTRUCTIONS_LENGTH = 4096;

    /** The id of the game in question. */
    public int gameId;

    /** The name of the creator of this game. */
    public MemberName creator;

    /** The item listed in the catalog for this game. */
    public Game listedItem;

    /** The source item maintained by the creator for this game. */
    public Game sourceItem;

    /** The creator supplied instructions for this game. */
    public String instructions;

    /** The total number of player games this game has accumulated. */
    public int gamesPlayed;

    /** The reported average duration of this game in seconds. */
    public int averageDuration;

    /** The minimum number of players for this game. */
    public int minPlayers;

    /** The maximum number of players for this game or Integer.MAX_VALUE if it's a party game. */
    public int maxPlayers;

    /** The number of people playing this game right now. */
    public int playingNow;

    /** Contains member rating and favorite information about the game. */
    public MemberItemInfo memberItemInfo = new MemberItemInfo();
    
    /** The height of a game screenshot. */
    public static final int SHOT_HEIGHT = 125;

    /** The width of a game screenshot. */
    public static final int SHOT_WIDTH = 175;

    /**
     * Returns the listed game if we have one, the source if not.
     */
    public Game getGame ()
    {
        return (listedItem == null) ? sourceItem : listedItem;
    }

    /**
     * Returns true if this is a party game, false otherwise.
     */
    public boolean isPartyGame ()
    {
        return maxPlayers == Integer.MAX_VALUE;
    }
}
