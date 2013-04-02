(ns boxy.repo
  "This namespace describes the internal representation of the mock repository and provides 
   functions that manipulate the repository in an immutable way.

   FIXME:  This repository does not consider the permissions of the connected user making the 
           requests."
  (:require [clojure-commons.file-utils :as file]))


(defn contains-entry?
  "Determines if the path points to an entry in the repository.

   Parameters:
     repo - The repository to inspect.
     path - The absolute path to check.

   Returns:
     It returns true if the path points an entry, otherwise false."
  [repo path]
  (contains? repo path))


(defn get-acl
  "Retrieves the ACL for the given entry in the repository.

   Parameters:
     repo - The repository to inspect.
     path - The absolute path to the entry to inspect.

   Returns:
     It returns a mapping of group and user Ids to their respective permission. Users without 
     permission are not returned."
  [repo path]
  (get-in repo [path :acl]))

  
(defn get-avus
  "Retrieves the set of AVUs for a given entry in the repository.

   Parameters:
     repo - The repository to inspect.
     path - the absolute path to the entry to inspect.

   Returns:
     If returns an unordered sequence of AVUs for the entry. The AVUs are represented as a triplet 
     of the form [attribute value unit]. If the attribute is unitless, unit will be the empty 
     string."
  [repo path]
  (let [avus (get-in repo [path :avus])]
    (map #(vec (cons % (get avus %))) 
         (keys avus))))


(defn get-create-time
  "Retrieves the time with the collection or data object was created in milliseconds since the POSIX
   epoch.

   Parameters:
     repo - the repository to inspect
     path - the absolute path the collection or data object

   Returns:
     The time in milliseconds since the POSIX epoch."
  [repo path]
  (get-in repo [path :create-time]))

  
(defn get-creator
  "Retrieves the user name and zone the creator of a collection of or data object.

   Parameters:
     repo - the repository to inspect
     path - the absolute path to the entry to inspect

   Returns:
     Returns a two element array.  The first element is the user name and the second is the zone."
  [repo path]
  (get-in repo [path :creator]))


(defn get-members
  "Retrieves the immediate members of a given directory.

   Parameters:
     repo - The repository to inspect.
     path - the absolute path to the directory.

   Returns:
     A list of absolute paths to the members"
  [repo parent-path]
  (let [re (re-pattern (str "^" (file/add-trailing-slash parent-path) "[^/]+$"))]
    (filter #(and (not (keyword? %)) 
                  (re-matches re %)) 
            (keys repo))))


(defn get-modify-time
  "Retrieves the time with the collection or data object was last modified in milliseconds since the 
   POSIX epoch.

   Parameters:
     repo - the repository to inspect
     path - the absolute path the collection or data object

   Returns:
     The time in milliseconds since the POSIX epoch."
  [repo path]
  (get-in repo [path :modify-time]))

  
(defn get-permission
  "Retrieves the access permission a user or group has for a given entry in the repository. 

   Parameters:
     repos - The repository to inspect
     path  - The path to the entry of interest
     user  - The user or group of interest
     zone  - The zone the user belongs to

   Returns:
     It returns :own if the user has ownership permission, :write if the user has write permission, 
     :read if the user has read permission, and nil if the user has no permission."
  [repo path user zone]
  (get-in repo [path :acl [user zone]]))
  
  
(defn get-type
  "Indicates the type of entry a path points to.

   Parameters:
     repo - The repository to inspect.
     path - The absolute path of the entry to check.

   Returns:
     It returns :file for a file, :normal-dir for a normal directory, or :linked-dir for a linked 
     directory."
  [repo path]
  (get-in repo [path :type]))


(defn get-user-groups
  "Retrieves the groups that a given user belongs to.

   Parameters:
     repo - The repository to inspect
     user - The name of the user
     zone - The zone the user belongs to

   Returns:
     It returns an unordered sequence of groups containing the user.  A group is a pair of the form 
     [group-name zone]."
  [repo user zone]
  (let [groups (:groups repo)]
    (filter #(contains? (get groups %) [user zone]) (keys groups))))


(defn user-exists?
  "Indicates whether or not there is an account for a given user name.

   Parameters:
     repo - The repository to inspect.
     user - The name of the user
     zone - The zone the user belongs to.

   Returns:
     It returns true if there is a user account with the given name, otherwise it returns false."
  [repo user zone]
  (contains? (:users repo) [user zone]))


(defn add-avu
  "Adds an AVU to an entry in the repository  An existing AVU with the same name will be replaced.

   Parameters:
     repo - The repository holding the entry
     path - The path to the entry to modify
     attr - The name of the attribute to add
     val  - The value of the attribute
     unit - The unit of the attribute. This should be the empty string if the attribute is unitless.

  Returns:
    It returns a copy of the repository with the new AVU."
  [repo path attr val unit]
  (assoc-in repo [path :avus attr] [val unit]))

  
(defn add-file
  "Adds a new, empty file to a repository. The file will have an empty ACL and no AVUs.

   Parameters:
     repo - The repository receiving the file
     path - The path into the repository of the new file
     creator - The user Id of the user creating the file
     create-time - The time the file is being created in milliseconds since the POSIX epoch.
     
   Returns:
     It returns a copy of the repository with the new file.

   FIXME:  If an entry exists at this path, it will overwrite the entry even if it is a directory.
   FIXME:  Set creator and create-time"
  [repo path]
  (assoc repo path {:type        :file
                    :creator     ["UNKNOWN" "UNKNOWN"]
                    :create-time 0
                    :modify-time 0
                    :acl         {}
                    :avus        {}
                    :content     ""}))


(defn write-to-file
  "For a given file, it adds the given content starting at the given offset, overwriting as 
   necessary.

   Parameters:
     repo - The repository containing the file being modified.
     path - The path to the file being modified.
     add-content - The new content as a string
     offset - The offset into the file where the content will be written.

   Returns:
     It returns a copy of the repository with the modified file.

   FIXME: Update modify time"
  [repo path add-content offset]
  (let [old-content  (get-in repo [path :content])
        old-size     (count old-content)
        old-content' (apply str 
                            old-content 
                            (repeat (max 0 (- offset old-size)) \space))
        new-content  (str (subs old-content' 0 offset) 
                          add-content 
                          (subs old-content' (min (count old-content') 
                                                  (+ offset (count add-content)))))]
    (assoc-in repo [path :content] new-content)))
