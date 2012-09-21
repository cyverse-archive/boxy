(ns boxy.repo
  "This namespace describes the internal representation of the mock repository
   and provides functions that manipulate the repository in an immutable way.

   The repository is represented as a map.  The keys of the map are the paths
   to the files and directories in the repository.  There is one special key, 
   :groups, that provides access to the group information.  

   {:groups              {groups-entry}
    \"/path/to/directory\" {directory-entry}
    \"/path/to/file\"      {file-entry}
   }

   The groups entry is a map of group Ids to the sets of users belonging to the
   respective groups.

   {[group-id] #{user-set}}

   A group id is a pair of strings, the first being the name of the group and
   the second being the zone the group belongs to.

   [\"group name\" \"zone\"]

   A user set, is just a set of user names.

   #{\"user1\" \"user2\"}

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
    :content \"file content\"}

   An ACL entry is a map of user names to their respective access permissions.  
   The allowed access permissions are :read for read permission, :write for read
   and write permission, and :own for read, write and ownership permissions.

   {\"read user\"  :read
    \"write user\" :write
    \"owner user\" :own}

   An AVU entry is a map from attribute names to their corresponding values and 
   units.  The values and units are stored as a pair of strings.  A unit of \"\"
   means unitless.  

   {\"attribute\" [\"value\" \"unit\"] \"unitless\" [\"value\" \"\"]} 

   Here's a full example.

   {:groups               {[\"group\" \"zone\"] #{\"user\"}} 
   \"/zone\"                {:type :dir
                           :acl  {}
                           :avus {}}
   \"/zone/home\"           {:type :dir
                           :acl  {\"group\" :read}
                           :avus {}}
   \"/zone/home/user\"      {:type :dir
                           :acl  {\"user\" :write}
                           :avus {}}
   \"/zone/home/user/file\" {:type    :file
                           :acl     {\"user\" :own}
                           :avus    {\"has-unit\" [\"value\" \"unit\"] 
                                     \"unitless\" [\"value\" \"\"]}
                           :content \"content\"}}

   FIXME:  This repository does not consider the permissions of the connected
           user making the requests"
  (:require [clojure-commons.file-utils :as cf]))


(defn contains-entry?
  "Determines if the path points to an entry in the repository.

   Parameters:
     repo - The repository to inspect.
     path - The absolute path to check.

   Returns:
     It returns true if the path points an entry, otherwise false."
  [repo path]
  (contains? repo (cf/rm-last-slash path)))


(defn is-dir?
  "Determines whether or not the given path points to a directory in the 
   repository.

   Parameters:
     repo - The repository to inspect.
     path - The absolute path of the entry to check.

   Returns:
     It returns true if the entry is a directory, otherwise false."
  [repo path]
  (= :dir (:type (get repo (cf/rm-last-slash path)))))


(defn is-file?
  "Determines whether or not the given path points to a file in the repository.

   Parameters:
     repo - The repository to inspect.
     path - The absolute path of the entry to check.

   Returns:
     It returns true if the entry is a file, otherwise false."
  [repo path]
  (= :file (:type (get repo path))))


(defn get-avus
  "Retrieves the set of AVUs for a given entry in the repository.

   Parameters:
     repo - The repository to inspect.
     path - the absolute path to the entry to inspect.

   Returns:
     If returns an unordered sequence of AVUs for the entry.  The AVUs are 
     represented as a triplet of the form [attribute value unit].  If the 
     attribute is unitless, unit will be the empty string."
  [repo path]
  (let [avus (:avus (get repo (cf/rm-last-slash path)))]
    (map #(vec (cons % (get avus %))) 
         (keys avus))))


(defn get-permission
  "Retrieves the access permission a user or group has for a given entry in the 
   repository. 

   Parameters:
     repos - The repository to inspect
     path  - The path to the entry of interest
     user  - The user or group of interest
     zone  - The zone the user belongs to

   Returns:
     It returns :own if the user has ownership permission, :write if the user
     has write permission, :read if the user has read permission, and nil if the
     user has no permission. 

   FIXME:  This does not consider zone."
  [repo path user zone]
  (-> repo (get path) :acl (get user)))


(defn get-user-groups
  "Retrieves the groups that a given user belongs to.

   Parameters:
     repo - The repository to inspect.
     user - The name of the user

   Returns:
     It returns an unordered sequence of groups containing the user.  A group
     is a pair of the form [group-name zone].
 
   FIXME:  This does not consider the user's zone."
  [repo user]
  (let [groups (:groups repo)]
    (filter #(contains? (get groups %) user) (keys groups))))


(defn add-avu
  "Adds an AVU to an entry in the repository  An existing AVU with the same name
   will be replaced.

   Parameters:
     repo - The repository holding the entry
     path - The path to the entry to modify
     attr - The name of the attribute to add
     val  - The value of the attribute
     unit - The unit of the attribute.  This should be the empty string if the
       attribute is unitless.

  Returns:
    It returns a copy of the repository with the new AVU."
  [repo path attr val unit]
  (let [old-entry (get repo path)
        old-avus  (:avus old-entry)
        new-avus  (assoc old-avus attr [val unit])
        new-entry (assoc old-entry :avus new-avus)]
    (assoc repo path new-entry)))

  
(defn add-file
  "Adds a new, empty file to a repository.  The file will have an empty ACL and
   no AVUs.

   Parameters:
     repo - The repository receiving the file
     path - The path into the repository of the new file
     
   Returns:
     It returns a copy of the repository with the new file.

   FIXME:  If an entry exists at this path, it will overwrite the entry even if
           it is a directory."
  [repo path]
  (assoc repo path {:type    :file
                    :acl     {}
                    :avus    {}
                    :content ""}))


(defn write-to-file
  "For a given file, it adds the given content starting at the given offset,
   overwriting as necessary.

   Parameters:
     repo - The repository containing the file being modified.
     path - The path to the file being modified.
     add-content - The new content as a string
     offset - The offset into the file where the content will be written.

   Returns:
     It returns a copy of the repository with the modified file."
  [repo path add-content offset]
  (let [old-entry    (get repo path)
        old-content  (:content old-entry)
        old-size     (count old-content)
        old-content' (apply str old-content 
                            (repeat (max 0 (- offset old-size)) \space))
        new-content  (str (subs old-content' 0 offset) 
                          add-content 
                          (subs old-content' (min (count old-content') 
                                                  (+ offset (count add-content)))))
        new-entry   (assoc old-entry :content new-content)]
    (assoc repo path new-entry)))
