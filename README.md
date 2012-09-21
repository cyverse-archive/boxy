# boxy

This is a library intended to back the clj-jargon with a configurable mock up of
an iRODS repository for unit testing purposes.

_Currently this is implementation is incomplete.  It will be extended as needed 
for unit testing on iPlant projects._


## Repository Representation

The repository is represented as a map.  The keys of the map are the paths to 
the files and directories in the repository.  There is one special key, :groups, 
that provides access to the group information.  

    {:groups              {groups-entry}
     "/path/to/directory" {directory-entry}
     "/path/to/file"      {file-entry}}

The groups entry is a map of group Ids to the sets of users belonging to the
respective groups.

    {[group-id] #{user-set}}

A group id is a pair of strings, the first being the name of the group and the 
second being the zone the group belongs to.

    ["group-name" "zone"]

A user set, is just a set of user names.

    #{"user-1" "user-2"}

The structure of a directory entry and a file entry are both maps with nearly
the same keys.  A file entry has one additional key, :content, that holds the
textual contents of the file.  The remaining keys, common to both, are as 
follows.  :type identifies whether the entry is a directory entry (:dir) or a
file entry (:file).  :acl provides the ACL for the entry.  Finally, :avus
provides the AVU metadata associated with the entry.

    {:type :dir
     :acl  {acl-entry}
     :avus {avus-entry}}

    {:type    :file
     :acl     {acl-entry}
     :avus    {avus-entry}
     :content "file content"}

An ACL entry is a map of user names to their respective access permissions.  The 
allowed access permissions are :read for read permission, :write for read and 
write permission, and :own for read, write and ownership permissions.

    {"read-user"  :read
     "write-user" :write
     "owner-user" :own}

An AVU entry is a map from attribute names to their corresponding values and 
units.  The values and units are stored as a pair of strings.  A unit of "" 
means unitless.  

    {"attribute" ["value" "unit"] "unitless" ["value" ""]} 

Here's a full example.

    {:groups                {["group" "zone"] #{"user"}} 
     "/zone"                {:type :dir
                             :acl  {}
                             :avus {}}
     "/zone/home"           {:type :dir
                             :acl  {"group" :read}
                             :avus {}}
     "/zone/home/user"      {:type :dir
                             :acl  {"user" :write}
                             :avus {}}
     "/zone/home/user/file" {:type    :file
                             :acl     {"user" :own}
                             :avus    {"has-unit" ["value" "unit"] 
                                       "unitless" ["value" ""]}
                             :content "content"}}


## iRODS Proxy

Unfortunately, the main entry point into Jargon library is through the concrete
class org.irods.jargon.core.pub.IRODSFileSystem.  To work around this lack of
interface, the irods-mock.jargon-if/IRODSProxy protocol was created.  Through 
duck typing, an instance of any record that extends this protocol may be used in 
place of an instance of IRODSFileSystem.


## Usage

Normally, the client would populate a content map with the test data.  The 
client would then pass this map to irods-mock.core/mk-mock-proxy function to
construct an IRODSProxy instance.  Under the hood, it constructs a MockProxy
object with appropriate constructors.

If the client would rather use something other than a content map to model an
iRODS repository, the MockProxy constructor may be used directly, passing in
constructors for the custom components.
