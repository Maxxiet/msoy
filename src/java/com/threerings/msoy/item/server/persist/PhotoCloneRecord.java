//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Table;
import com.samskivert.jdbc.depot.annotation.TableGenerator;

/** Clone records for Photos. */
@Entity
@Table
@TableGenerator(name="cloneId", allocationSize=-1,
                initialValue=-1, pkColumnValue="PHOTO_CLONE")
public class PhotoCloneRecord extends CloneRecord<PhotoRecord>
{
}
