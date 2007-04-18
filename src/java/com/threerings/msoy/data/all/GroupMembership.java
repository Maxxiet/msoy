//
// $Id$

package com.threerings.msoy.data.all;

import java.util.Date;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.io.Streamable;
import com.threerings.presents.dobj.DSet;
import com.threerings.util.ActionScript;

/**
 * Summarizes a person's membership in a group.
 */
public class GroupMembership
    implements Streamable, IsSerializable, DSet.Entry
{
    /** Not ever stored in a GroupMembership record, but useful for methods
     * that return a user's rank as a byte. */
    public static final byte RANK_NON_MEMBER = 0;

    /** Membership ranks. */
    public static final byte RANK_MEMBER = 1;
    public static final byte RANK_MANAGER = 2;
    
    /** The name and id of the member of the group. <em>Note:</em> this will be null in the records
     * maintained in a member's MemberObject. */
    public MemberName member;

    /** The group's identity. <em>Note:</em> this will be null in the records contained in a 
     * GroupDetail.members list.*/
    public GroupName group;

    /** The member's rank in the group. */
    public byte rank; 

    /** The date this member's rank was assigned, as represented by java.util.Date.getTime() */
    public long rankAssignedDate;

    /**
     * Returns true if the supplied rank is a valid rank (not {@link #RANK_NON_MEMBER} or an
     * otherwise invalid number.
     */
    public static boolean isValidRank (byte rank)
    {
        return rank >= RANK_MEMBER && rank <= RANK_MANAGER;
    }

    /** 
     * Get the date this member's rank was assigned on as a Date object.
     */
    @ActionScript(omit=true)
    public Date getRankAssignedDate () 
    {
        return new Date(rankAssignedDate);
    }

    // from DSet.Entry
    public Comparable getKey ()
    {
        return group;
    }
}
