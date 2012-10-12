(ns boxy.core
  "This namespace provides an implementation of the Jargon classes for 
   interacting with a mock up of an iRODS repository.  The function 
   mk-mock-proxy should normally be the entry point."
  (:require [clojure-commons.file-utils :as cf]
            [boxy.jargon-if :as j]
            [boxy.repo :as r])
  (:import [java.util List]
           [org.irods.jargon.core.exception DataNotFoundException
                                            FileNotFoundException]
           [org.irods.jargon.core.protovalues FilePermissionEnum]
           [org.irods.jargon.core.pub CollectionAO
                                      CollectionAndDataObjectListAndSearchAO
                                      DataObjectAO
                                      IRODSAccessObjectFactory
                                      IRODSFileSystemAO
                                      QuotaAO
                                      UserAO
                                      UserGroupAO]
           [org.irods.jargon.core.pub.domain ObjStat
                                             ObjStat$SpecColType
                                             User
                                             UserGroup]
           [org.irods.jargon.core.pub.io FileIOOperations
                                         IRODSFile
                                         IRODSFileFactory
                                         IRODSFileOutputStream]
           [org.irods.jargon.core.query MetaDataAndDomainData
                                        MetaDataAndDomainData$MetadataDomain]))


(defrecord MockFile [repo-ref account path]
  ^{:doc
    "This is description of a file or directory in a mutable boxy repository.

     Parameters:
       repo-ref - An atom containing the repository content.
       account - The IRODSAccount identifying the connected user.
       path - The path to file or directory.

     NOTE:  This has been implemented only enough to support current testing."}
  IRODSFile
  
  (close [_])
  
  (delete [_]
    "TODO: implement this method"
    false)
  
  (createNewFile [_]
    (if (r/contains-entry? @repo-ref path)
      false
      (do
        (reset! repo-ref (r/add-file @repo-ref path))
        true)))
  
  (exists [_]
    (r/contains-entry? @repo-ref (cf/rm-last-slash path)))
  
  (getAbsolutePath [_]
    (cf/rm-last-slash path))
  
  (getFileDescriptor [_]
    3)
  
  (getParent [_]
    (cf/rm-last-slash (cf/dirname path)))
  
  (initializeObjStatForFile [_]
    "NOTE:  The returned ObjStat instace only indentifes the SpecColType of the
     entry the path points to."
    (doto (ObjStat.)
      (.setSpecColType (condp = (r/get-type @repo-ref (cf/rm-last-slash path))
                         :normal-dir  ObjStat$SpecColType/NORMAL
                         :linked-dir  ObjStat$SpecColType/LINKED_COLL
                                      nil))))
        
  (isDirectory [_]
    (let [type (r/get-type @repo-ref (cf/rm-last-slash path))]
      (or (= :normal-dir type) (= :linked-dir type))))
  
  (isFile [_]
    (= :file (r/get-type @repo-ref path)))
  
  (mkdirs [_]
    "TODO: implement this method"
    false)
  
  (reset [_]
    #_"TODO implement this method"))


(defrecord MockFileIOOperations [repo-ref account file]
  ^{:doc
    "An implementation of file I/O operations for a given file in a mutable 
     boxy repository.

     Parameters:
       repo-ref - An atom containing the repository content.
       account - The IRODSAccount identifying the connected user.
       file - The IRODSFile describing the file of interest..

     NOTE:  This assumes that the String is an 8 bit character set.
     NOTE:  This has been implemented only enough to support current testing."}
  FileIOOperations
  
  (write [_ fd buffer offset length]
    ^{:doc "This assures that buffer is a character array with 8 bit characters."}
    (let [path        (.getAbsolutePath file)
          add-content (String. buffer offset length)]
      (reset! repo-ref (r/write-to-file @repo-ref path add-content offset))
      length)))


(defrecord MockFileFactory [file-ctor io-ops-ctor]
  ^{:doc
    "This is factory for producing java.io-like objects for manipulating fake 
     iRODS data.  It should normally be constructed by the mk-mock-file-factory 
     function.  If custom implementations of IRODSFile or FileIOOperations are 
     needed, they can be provided by passing the relevant constructors directly 
     to the MockFileFactory constructor.

     Parameters:
       file-ctor - This is a function that constructs an IRODSFile for a given
         path.  It takes an absolute path as its only argument.
       io-ops-ctor - This is a function that constructs a FileIOOperations for a 
         given IRODSFile object.  It takes an IRODSFile as its only argument.

     NOTE:  This has been implemented only enough to support current testing."}
  IRODSFileFactory

  (^IRODSFile instanceIRODSFile [_ ^String path]
    (file-ctor path))

  (^IRODSFileOutputStream instanceIRODSFileOutputStream [_ ^IRODSFile file]
    (proxy [IRODSFileOutputStream] [file (io-ops-ctor file)])))


(defn mk-mock-file-factory
  "Constructs a MockFileFactory backed by a mutable boxy repository.
 
   Parameters:
     repo-ref - An atom containing the content.
     acnt - The IRODSAccount object representing the connected user.
  
   Returns:
     It returns a MockFileFactory instance."
  [repo-ref acnt]
  (->MockFileFactory (partial ->MockFile repo-ref acnt)
                     (partial ->MockFileIOOperations repo-ref acnt)))
  
  
(defrecord MockFileSystemAO [repo-ref account]
  ^{:doc
    "This is an iRODS file system accesser that is backed by mutable repository 
     content.

     Parameters:
       repo-ref - An atom containing the content.
       account - The IRODSAccount identifying the connected user."}
  IRODSFileSystemAO
  
  (getListInDir [_ file]
    (if-not (.exists file)
      (throw (FileNotFoundException. (str (.getAbsolutePath file) " not found")))
      (map #(cf/basename %) (r/get-members @repo-ref (if (.isDirectory file)
                                                       (.getAbsolutePath file)
                                                       (.getParent file)))))))


(defrecord MockEntryListAO [repo-ref account]
  ^{:doc
    "This is an iRODS entry lister and searcher that is backed by a mutable 
     repository content.

     Parameters:
       repo-ref - An atom containing the content.
       account - The IRODSAccount identifying the connected user."}
  CollectionAndDataObjectListAndSearchAO)
  

(defrecord MockCollectionAO [repo-ref account]
  ^{:doc
    "This is a collections accesser that is backed by mutable repository content.

     Parameters:
       repo-ref - An atom containing the content.
       account - The IRODSAccount identifying the connected user."}
  CollectionAO
  
  (getPermissionForCollection [_ path user zone]
    "FIXME:  This doesn't check to see if the path points to a data object."
    (condp = (r/get-permission @repo-ref (cf/rm-last-slash path) user zone)
      :own   FilePermissionEnum/OWN
      :read  FilePermissionEnum/READ
      :write FilePermissionEnum/WRITE
      nil    FilePermissionEnum/NONE)))


(defrecord MockDataObjectAO [repo-ref account]
  ^{:doc
    "This is a data objects accesser that is backed by mutable repository 
     content.

     Parameters:
       repo-ref - An atom containing the content.
       account - The IRODSAccount identifying the connected user.
    
     NOTE:  This has been implemented only enough to support current testing."}
  DataObjectAO
  
  (addAVUMetadata [_ path avu]
    (reset! repo-ref 
            (r/add-avu @repo-ref 
                     (cf/rm-last-slash path) 
                     (.getAttribute avu) 
                     (.getValue avu) 
                     (.getUnit avu))))
    
  (^List findMetadataValuesForDataObject [_ ^String path]
    "NOTE:  I don't know what a domain object Id or unique name are, so I'm 
            using the path for both."
    "FIXME:  This doesn't check to see if the path points to a collection."
    (let [path' (cf/rm-last-slash path)]
      (map #(MetaDataAndDomainData/instance 
              MetaDataAndDomainData$MetadataDomain/DATA
              path'
              path'
              (first %)
              (second %)
              (last %))
           (r/get-avus @repo-ref path'))))
  
  (getPermissionForDataObject [_ path user zone]
    "FIXME:  This doesn't check to see if the path points to a collection."
    (condp = (r/get-permission @repo-ref (cf/rm-last-slash path) user zone)
      :own   FilePermissionEnum/OWN
      :read  FilePermissionEnum/READ
      :write FilePermissionEnum/WRITE
      nil    FilePermissionEnum/NONE))
  
  (setAccessPermissionOwn [_ zone path user])
    #_"TODO: implement")

  
(defrecord MockGroupAO [repo-ref account]
  ^{:doc
    "This is a groups accesser that is backed by mutable repository content.

     Parameters:
       repo-ref - An atom containing the content.
       account - The IRODSAccount identifying the connected user.

     NOTE:  This has been implemented only enough to support current testing."}  
  UserGroupAO
  
  (findUserGroupsForUser [_ user]
    "NOTE:  I don't know what a group Id is, so I'm setting it to name:zone."
    (let [mk-UserGroup (fn [group]
                         (let [name (first group)
                               zone (second group)]
                           (doto (UserGroup.)
                             (.setUserGroupId (str name ":" zone))
                             (.setUserGroupName name)
                             (.setZone zone))))]
      (map mk-UserGroup (r/get-user-groups @repo-ref user)))))

  
(defrecord MockUserAO [repo-ref account]
  ^{:doc
    "This is a users accesser that is backed by mutable repository content.

     Parameters:
       repo-ref - An atom containing the content.
       account - The IRODSAccount identifying the connected user.

     NOTE:  This has been implemented only enough to support current testing."}  
  UserAO
  
  (findByName [_ name]
    "NOTE:  This function does not set the comment, createTime, Id, Info, 
       modifyTime, userDN, userType or zone fields in the returned User object."
    (if-not (r/user-exists? @repo-ref name)
      (throw (DataNotFoundException. "unknown user"))
      (doto (User.)
        (.setName name)))))


(defrecord MockQuotaAO [repo-ref account]
  ^{:doc
    "This is a quotas accesser that is backed by mutable repository content.

     Parameters:
       repo-ref - An atom containing the content.
       account - The IRODSAccount identifying the connected user."}
  QuotaAO)


(defrecord MockAOFactory [fs-ctor 
                          entry-list-ctor
                          collection-ctor 
                          data-obj-ctor 
                          group-ctor
                          user-ctor
                          quota-ctor]
  ^{:doc 
    "This is an accesser factory that creates account dependent accessors from 
     provided constructors.  Though not required, it is intended that created 
     accessers are backed by a mutable content map.  

     Factory objects should normally be constructed by calling 
     mk-mock-ao-factory.  If custom implementations of one or more of the 
     underlying accessers are needed,they can be provided by passing the 
     relevant constructors directly to the MockAOFactory constructor.

     Parameters:
       fs-ctor - The constructor for IRODSFileSystemAO objects.  As its only 
         argument, it should accept an IRODSAccount identifying the connected 
         user.
       entry-list-ctor - The constructor for 
         CollectionAndDataObjectListAndSearchAO objects.  As its only argument, 
         it should accept an IRODSAccount identifying the connected user.
       collection-ctor - The constructor for CollectionAO objects.  As its only 
         argument, it should accept an IRODSAccount identifying the connected 
         user.
       data-obj-ctor - The constructor for DataObjectAO objects.  As its only 
         argument, it should accept an IRODSAccount identifying the connected 
         user.
       group-ctor - The constructor for UserGroupAO objects.  As its only 
         argument, it should accept an IRODSAccount identifying the connected 
         user.
       user-ctor - The constructor for UserGAO objects.  As its only argument, 
         it should accept an IRODSAccount identifying the connected user.
       quota-ctor - The constructor for QuotaAO objects.  As its only argument, 
         it should accept an IRODSAccount identifying the connected user.

     NOTE:  This has been implemented only enough to support current testing."}
  IRODSAccessObjectFactory

  (getCollectionAO [_ acnt] 
    (collection-ctor acnt))

  (getCollectionAndDataObjectListAndSearchAO [_ acnt] 
    (entry-list-ctor acnt))

  (getDataObjectAO [_ acnt] 
    (data-obj-ctor acnt))

  (getIRODSFileSystemAO [_ acnt] 
    (fs-ctor acnt))

  (getQuotaAO [_ acnt] 
    (quota-ctor acnt))

  (getUserAO [_ acnt] 
    (user-ctor acnt))
  
  (getUserGroupAO [_ acnt] 
    (group-ctor acnt)))


(defn mk-mock-ao-factory
  "Constructs a MockAOFactory with mutable repository content.
 
   Parameters:
     repo-ref - An atom containing the content.
  
   Returns:
     It returns a MockProxy instance."
  [repo-ref]
  (->MockAOFactory (partial ->MockFileSystemAO repo-ref)
                   (partial ->MockEntryListAO repo-ref)
                   (partial ->MockCollectionAO repo-ref)
                   (partial ->MockDataObjectAO repo-ref)
                   (partial ->MockGroupAO repo-ref)
                   (partial ->MockUserAO repo-ref)
                   (partial ->MockQuotaAO repo-ref)))
  

(defrecord MockProxy [ao-factory-ctor file-factory-ctor]
  ^{:doc
    "This is an iRODS proxy that provides access to fake iRODS data.  It should
     normally be constructed by the mk-mock-proxy function.  If custom 
     implementations of IRODSAccessObjectFactory or IRODSFileFactory are needed,
     they can be provided by passing the relevant constructors directly to the
     MockProxy constructor.

     Parameters:
       ao-factory-ctor - This is a zero argument function that constructs an
         IRODSAccessObjectFactory.
       file-factory-ctor - This is a function that constructs an 
         IRODSFileFactory for a given iRODS account.  It takes an IRODSAccount 
         as its only argument.

     NOTE:  This has been implemented only enough to support current testing."}
  j/IRODSProxy
  
  (close [_])

  (getIRODSAccessObjectFactory [_] 
    (ao-factory-ctor))

  (getIRODSFileFactory [_ acnt] 
    (file-factory-ctor acnt)))
            

(defn mk-mock-proxy
  "Constructs a MockProxy with mutable repository content.
 
   Parameters:
     repo-ref - An atom containing the content.
  
   Returns:
     It returns a MockProxy instance."
  [repo-ref]
  (->MockProxy #(mk-mock-ao-factory repo-ref) 
               (partial mk-mock-file-factory repo-ref)))
