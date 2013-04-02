# boxy

This is a library intended to back the clj-jargon with a configurable mock up of an iRODS repository 
for unit testing purposes.

_Currently this is implementation is incomplete. It will be extended as needed for unit testing on 
iPlant projects._


## Repository Representation

The repository is represented as a map. The keys of the map are the paths to the files and 
directories in the repository. There is two special keys: `:users` provides access to the user 
information and `:groups` provides access to the group information.  

    {:users               #{user-set}
     :groups              {groups-entry}
     "/path/to/directory" {directory-entry}
     "/path/to/file"      {file-entry}}

A user set, is just a set of user Ids.

    #{[user-id]}

A user Id is a pair of strings, the first being the name of the user and the second being the zone 
the user belongs to.

    ["user-name" "zone"]

The groups entry is a map of group Ids to the sets of users belonging to the respective groups.

    {[group-id] #{user-set}}

Like a user Id, a group Id is a pair of strings, the first being the name of the group and the 
second being the zone the group belongs to.

    ["group-name" "zone"]

The structure of a directory entry and a file entry are both maps with nearly the same keys. A file 
entry has one additional key, `:content`, that holds the textual contents of the file. The remaining 
keys, common to both, are as follows. `:type` identifies whether the entry is a normal directory 
entry (`:normal-dir`), a linked directory (`:linked-dir`), or a file (`:file`). `:creator` is the
user Id of the person who created the entry. `:create-time` is the time when the entry was created
in milliseconds since the POSIX epoch. `:modify-time` is the time when the entry was last modified
in milliseconds since the POSIX epoch. `:acl` provides the ACL for the entry. Finally, `:avus` 
provides the AVU metadata associated with the entry.

    {:type        :normal-dir
     :creator     [user-id]
     :create-time 0
     :modify-time 0
     :acl         {acl-entry}
     :avus        {avus-entry}}

    {:type        :linked-dir
     :creator     [user-id]
     :create-time 1
     :modify-time 1
     :acl         {acl-entry}
     :avus        {avus-entry}}

    {:type        :file
     :creator     [user-id]
     :create-time 2
     :modify-time 3
     :acl         {acl-entry}
     :avus        {avus-entry}
     :content     "file content"}

An ACL entry is a map of group and user Ids to their respective access permissions. The allowed 
access permissions are `:read` for read permission, `:write` for read and write permission, and 
`:own` for read, write and ownership permissions.

    {[reader-id] :read
     [writer-id] :write
     [owner-id]  :own}

An AVU entry is a map from attribute names to their corresponding values and units. The values and 
units are stored as a pair of strings. A unit of `""` means unitless.  

    {"attribute" ["value" "unit"] "unitless" ["value" ""]} 

Here's a full example.

    {:users                   #{["user1" "zone1"] ["user2" "zone1"]}
     :groups                  {["group" "zone1"] #{"user1"}} 
     "/zone1"                 {:type        :dir
                               :creator     ["user1" "zone1"]
                               :create-time 0
                               :modify-time 0
                               :acl         {}
                               :avus        {}}
     "/zone1/home"            {:type :dir
                               :creator     ["user1" "zone1"]
                               :create-time 0
                               :modify-time 0
                               :acl  {["group" "zone1"] :read}
                               :avus {}}
     "/zone1/home/user1"      {:type :dir
                               :creator     ["user1" "zone1"]
                               :create-time 0
                               :modify-time 0
                               :acl  {["user1" "zone1"] :write}
                               :avus {}}
     "/zone1/home/user1/file" {:type        :file
                               :creator     ["user1" "zone1"]
                               :create-time 1
                               :modify-time 2
                               :acl         {["user2" "zone1"] :own}
                               :avus        {"has-unit" ["value" "unit"] "unitless" ["value" ""]}
                               :content     "content"}
     "/zone1/home/user1/link" {:type        :linked-dir
                               :creator     ["user1" "zone1"]
                               :create-time 3
                               :modify-time 3
                               :acl         {}
                               :avus        {}}}}


## iRODS Proxy

Unfortunately, the main entry point into Jargon library is through the concrete class 
`org.irods.jargon.core.pub.IRODSFileSystem`. To work around this lack of interface, the 
`irods-mock.jargon-if/IRODSProxy` protocol was created.  Through duck typing, an instance of any 
record that extends this protocol may be used in place of an instance of `IRODSFileSystem`.


## Usage

Normally, the client would populate a content map with the test data. The client would then pass 
this map to `irods-mock.core/mk-mock-proxy` function to construct an `IRODSProxy` instance. Under 
the hood, it constructs a `MockProxy` object with appropriate constructors.

If the client would rather use something other than a content map to model an iRODS repository, the 
`MockProxy` constructor may be used directly, passing in constructors for the custom components.
