//
// $Id$

package com.threerings.msoy.web.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.IntMap;
import com.samskivert.util.IntSet;
import com.samskivert.util.Predicate;
import com.samskivert.util.Tuple;
import com.samskivert.util.StringUtil;

import com.threerings.parlor.rating.server.persist.RatingRecord;

import com.threerings.msoy.data.UserAction;

import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.MemberNodeActions;
import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.persist.MemberNameRecord;
import com.threerings.msoy.server.persist.MemberRecord;

import com.threerings.msoy.game.data.all.Trophy;
import com.threerings.msoy.game.server.persist.TrophyRecord;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.item.server.persist.GameRecord;

import com.threerings.msoy.person.data.BlurbData;
import com.threerings.msoy.person.data.Profile;
import com.threerings.msoy.person.data.ProfileCard;
import com.threerings.msoy.person.data.ProfileLayout;
import com.threerings.msoy.person.server.persist.ProfileRecord;

import com.threerings.msoy.group.data.Group;
import com.threerings.msoy.group.data.GroupMembership;
import com.threerings.msoy.group.server.persist.GroupMembershipRecord;
import com.threerings.msoy.group.server.persist.GroupRecord;

import com.threerings.msoy.web.client.ProfileService;
import com.threerings.msoy.web.data.GameRating;
import com.threerings.msoy.web.data.GroupCard;
import com.threerings.msoy.web.data.MemberCard;
import com.threerings.msoy.web.data.ServiceCodes;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebIdent;

import static com.threerings.msoy.Log.log;

/**
 * Provides the server implementation of {@link ProfileService}.
 */
public class ProfileServlet extends MsoyServiceServlet
    implements ProfileService
{
    // from interface ProfileService
    public void updateProfile (WebIdent ident, String displayName, Profile profile)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser(ident);

        if (displayName != null) {
            displayName = displayName.trim();
        }
        if (!Profile.isValidDisplayName(displayName)) {
            // you'll only see this with a hacked client
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        // TODO: whatever filtering and profanity checking that we want

        try {
            // load their old profile record for "first time configuration" purposes
            ProfileRecord oprof = MsoyServer.profileRepo.loadProfile(memrec.memberId);

            // stuff their updated profile data into the database
            ProfileRecord nrec = new ProfileRecord(memrec.memberId, profile);
            if (oprof != null) {
                nrec.modifications = oprof.modifications+1;
                nrec.realName = oprof.realName;
            } else {
                log.warning("Account missing old profile [id=" + memrec.memberId + "].");
            }
            MsoyServer.profileRepo.storeProfile(nrec);

            // record that the user updated their profile
            logUserAction(memrec, (nrec.modifications == 1) ?
                          UserAction.CREATED_PROFILE : UserAction.UPDATED_PROFILE, null);
            _eventLogger.profileUpdated(memrec.memberId);

            // handle a display name change if necessary
            if (memrec.name == null || !memrec.name.equals(displayName)) {
                MsoyServer.memberRepo.configureDisplayName(memrec.memberId, displayName);
                // let the world servers know about the display name change
                MemberNodeActions.displayNameChanged(new MemberName(displayName, memrec.memberId));
            }

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to update member's profile " +
                    "[who=" + memrec.who() +
                    ", profile=" + StringUtil.fieldsToString(profile) + "].", pe);
        }
    }

    // from interface ProfileService
    public ProfileResult loadProfile (WebIdent ident, int memberId)
        throws ServiceException
    {
        MemberRecord memrec = getAuthedUser(ident);

        try {
            MemberRecord tgtrec = MsoyServer.memberRepo.loadMember(memberId);
            if (tgtrec == null) {
                return null;
            }

            ProfileResult result = new ProfileResult();
            result.name = tgtrec.getName();
            result.layout = loadLayout(tgtrec);

            // resolve the data for whichever blurbs are active on this player's profile page
            for (Object bdata : result.layout.blurbs) {
                BlurbData blurb = (BlurbData)bdata;
                switch (blurb.type) {
                case BlurbData.PROFILE:
                    result.profile = resolveProfileData(memrec, tgtrec);
                    break;

                case BlurbData.FRIENDS:
                    result.friends = resolveFriendsData(memrec, tgtrec);
                    IntSet friendIds = MsoyServer.memberRepo.loadFriendIds(tgtrec.memberId);
                    result.isOurFriend = (memrec != null) && friendIds.contains(memrec.memberId);
                    result.totalFriendCount = friendIds.size();
                    break;

                case BlurbData.GROUPS:
                    result.groups = resolveGroupsData(memrec, tgtrec);
                    break;

                case BlurbData.RATINGS:
                    result.ratings = resolveRatingsData(memrec, tgtrec);
                    break;

                case BlurbData.TROPHIES:
                    result.trophies = resolveTrophyData(memrec, tgtrec);
                    break;

                default:
                    log.log(Level.WARNING, "Requested to resolve unknown blurb " + bdata + ".");
                    break;
                }
            }
            return result;

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failure resolving blurbs [who=" + memberId + "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface ProfileService
    public List<ProfileCard> findProfiles (String type, String search)
        throws ServiceException
    {
        try {
            // locate the members that match the supplied search
            IntMap<ProfileCard> cards = new HashIntMap<ProfileCard>();
            if ("email".equals(type)) {
                MemberRecord memrec = MsoyServer.memberRepo.loadMember(search);
                if (memrec != null) {
                    ProfileCard card = new ProfileCard();
                    card.name = new MemberName(memrec.name, memrec.memberId);
                    cards.put(memrec.memberId, card);
                }

            } else {
                List<MemberNameRecord> names = null;
                if ("display".equals(type)) {
                    names = MsoyServer.memberRepo.findMemberNames(search, MAX_PROFILE_MATCHES);
                } else if ("name".equals(type)) {
                    names = MsoyServer.profileRepo.findMemberNames(search, MAX_PROFILE_MATCHES);
                }
                if (names != null) {
                    for (MemberNameRecord mname : names) {
                        ProfileCard card = new ProfileCard();
                        card.name = mname.toMemberName();
                        cards.put(mname.memberId, card);
                    }
                }
            }

            // load up their profile data
            resolveCardData(cards);

            ArrayList<ProfileCard> results = new ArrayList<ProfileCard>();
            results.addAll(cards.values());
            return results;

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failure finding profiles [search=" + search + "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface ProfileService
    public FriendsResult loadFriends (WebIdent ident, int memberId)
        throws ServiceException
    {
        MemberRecord mrec = getAuthedUser(ident);

        try {
            MemberRecord tgtrec = MsoyServer.memberRepo.loadMember(memberId);
            if (tgtrec == null) {
                return null;
            }

            FriendsResult result = new FriendsResult();
            result.name = tgtrec.getName();
            List<ProfileCard> list = MsoyServer.memberRepo.loadFriendCards(memberId);
            Collections.sort(list, new Comparator<ProfileCard>() {
                public int compare (ProfileCard c1, ProfileCard c2) {
                    return MemberName.compareNames(c1.name, c2.name);
                }
            });
            result.friends = list;
            return result;

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failure loading friends [memId=" + memberId + "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    protected ProfileLayout loadLayout (MemberRecord memrec)
        throws PersistenceException
    {
        // TODO: store this metadata in a database, allow it to be modified
        ProfileLayout layout = new ProfileLayout();
        layout.layout = ProfileLayout.TWO_COLUMN_LAYOUT;

        ArrayList<BlurbData> blurbs = new ArrayList<BlurbData>();
        BlurbData blurb = new BlurbData();
        blurb.type = BlurbData.PROFILE;
        blurb.blurbId = 0;
        blurbs.add(blurb);

        blurb = new BlurbData();
        blurb.type = BlurbData.FRIENDS;
        blurb.blurbId = 1;
        blurbs.add(blurb);

        blurb = new BlurbData();
        blurb.type = BlurbData.RATINGS;
        blurb.blurbId = 2;
        blurbs.add(blurb);

        blurb = new BlurbData();
        blurb.type = BlurbData.TROPHIES;
        blurb.blurbId = 3;
        blurbs.add(blurb);

        blurb = new BlurbData();
        blurb.type = BlurbData.GROUPS;
        blurb.blurbId = 4;
        blurbs.add(blurb);

        layout.blurbs = blurbs;
        return layout;
    }

    protected Profile resolveProfileData (MemberRecord reqrec, MemberRecord tgtrec)
        throws PersistenceException
    {
        ProfileRecord prec = MsoyServer.profileRepo.loadProfile(tgtrec.memberId);
        int forMemberId = (reqrec == null) ? 0 : reqrec.memberId;
        Profile profile = (prec == null) ? new Profile() : prec.toProfile(tgtrec, forMemberId);

        // TODO: if they're online right now, show that

        return profile;
    }

    protected List<MemberCard> resolveFriendsData (MemberRecord reqrec, MemberRecord tgtrec)
        throws PersistenceException
    {
        IntMap<MemberCard> cards = new HashIntMap<MemberCard>();
        for (FriendEntry entry :
                 MsoyServer.memberRepo.loadFriends(tgtrec.memberId, MAX_PROFILE_FRIENDS)) {
            MemberCard card = new MemberCard();
            card.name = entry.name;
            cards.put(entry.name.getMemberId(), card);
        }
        resolveCardData(cards);

        ArrayList<MemberCard> results = new ArrayList<MemberCard>();
        results.addAll(cards.values());
        return results;
    }

    protected List<GroupCard> resolveGroupsData (MemberRecord reqrec, MemberRecord tgtrec)
        throws PersistenceException
    {
        boolean showExclusive = (reqrec != null && reqrec.memberId == tgtrec.memberId);
        return MsoyServer.groupRepo.getMemberGroups(tgtrec.memberId, showExclusive);
    }

    protected List<GameRating> resolveRatingsData (MemberRecord reqrec, MemberRecord tgtrec)
        throws PersistenceException
    {
        // fetch all the rating records for the user
        List<RatingRecord> ratings = MsoyServer.ratingRepo.getRatings(
            tgtrec.memberId, -1, MAX_PROFILE_GAMES);

        // sort them by rating
        Collections.sort(ratings, new Comparator<RatingRecord>() {
            public int compare (RatingRecord o1, RatingRecord o2) {
                return (o1.rating > o2.rating) ? -1 : (o1.rating == o2.rating) ? 0 : 1;
            }
        });

        // create GameRating records for all the games we know about
        List<GameRating> result = new ArrayList<GameRating>();
        IntMap<GameRating> map = new HashIntMap<GameRating>();
        for (RatingRecord record : ratings) {
            GameRating rrec = map.get(Math.abs(record.gameId));
            if (rrec == null) {
                // stop adding results
                if (result.size() >= MAX_PROFILE_MATCHES) {
                    continue;
                }
                rrec = new GameRating();
                rrec.gameId = Math.abs(record.gameId);
                result.add(rrec);
                map.put(rrec.gameId, rrec);
            }
            if (record.gameId < 0) {
                rrec.singleRating = record.rating;
            } else {
                rrec.multiRating = record.rating;
            }
        }

        // now load up and fill in the game details
        for (IntMap.IntEntry<GameRating> entry : map.intEntrySet()) {
            int gameId = entry.getIntKey();
            GameRecord record = MsoyServer.itemMan.getGameRepository().loadGameRecord(gameId);
            if (record == null) {
                log.warning("Player has rating for non-existent game [id=" + gameId + "].");
                entry.getValue().gameName = "";
            } else {
                entry.getValue().gameName = record.name;
                if (record.thumbMediaHash != null) {
                    entry.getValue().gameThumb = new MediaDesc(
                        record.thumbMediaHash, record.thumbMimeType, record.thumbConstraint);
                }
            }
        }

        return result;
    }

    protected List<Trophy> resolveTrophyData (MemberRecord reqrec, MemberRecord tgtrec)
        throws PersistenceException
    {
        ArrayList<Trophy> list = new ArrayList<Trophy>();
        for (TrophyRecord record :
                 MsoyServer.trophyRepo.loadRecentTrophies(tgtrec.memberId, MAX_PROFILE_TROPHIES)) {
            list.add(record.toTrophy());
        }
        return list;
    }

    protected static void resolveCardData (IntMap<? extends MemberCard> cards)
        throws PersistenceException
    {
        for (ProfileRecord profile : MsoyServer.profileRepo.loadProfiles(cards.intKeySet())) {
            MemberCard card = cards.get(profile.memberId);
            card.photo = profile.getPhoto();
            if (card instanceof ProfileCard) {
                ((ProfileCard)card).headline = profile.headline;
            }
        }
    }

    protected static final int MAX_PROFILE_MATCHES = 100;
    protected static final int MAX_PROFILE_FRIENDS = 6;
    protected static final int MAX_PROFILE_GAMES = 10;
    protected static final int MAX_PROFILE_TROPHIES = 6;
}
