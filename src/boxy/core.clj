(ns boxy.core
  "This namespace provides an implementation of the Jargon classes for 
   interacting with a mock up of an iRODS repository."
  (:require [boxy.jargon-if :as ji])
  (:import [org.irods.jargon.core.pub CollectionAO
                                      CollectionAndDataObjectListAndSearchAO
                                      DataObjectAO
                                      IRODSAccessObjectFactory
                                      IRODSFileSystemAO
                                      QuotaAO
                                      UserAO
                                      UserGroupAO]
           [org.irods.jargon.core.pub.io IRODSFileFactory]))


;TODO define the content map


(defrecord MockFileFactory [content-ref account]
  ^{:doc 
    "This is an iRODS file factory that is backed by a mutable content map.

     Parameters:
       content-ref - An atom containing the content.
       account - The IRODSAccount identifying the connected user."}
  IRODSFileFactory)


(defrecord MockFileSystemAO [content-ref account]
  ^{:doc
    "This is an iRODS file system accesser that is backed by a mutable content 
     map.

     Parameters:
       content-ref - An atom containing the content.
       account - The IRODSAccount identifying the connected user."}
  IRODSFileSystemAO)


(defrecord MockEntryListAO [content-ref account]
  ^{:doc
    "This is an iRODS entry lister and searcher that is backed by a mutable 
     content map.

     Parameters:
       content-ref - An atom containing the content.
       account - The IRODSAccount identifying the connected user."}
  CollectionAndDataObjectListAndSearchAO)
  

(defrecord MockCollectionAO [content-ref account]
  ^{:doc
    "This is a collections accesser that is backed by a mutable content map.

     Parameters:
       content-ref - An atom containing the content.
       account - The IRODSAccount identifying the connected user."}
  CollectionAO)


(defrecord MockDataObjectAO [content-ref account]
  ^{:doc
    "This is a data objects accesser that is backed by a mutable content map.

     Parameters:
       content-ref - An atom containing the content.
       account - The IRODSAccount identifying the connected user."}
  DataObjectAO)


(defrecord MockGroupAO [content-ref account]
  ^{:doc
    "This is a groups accesser that is backed by a mutable content map.

     Parameters:
       content-ref - An atom containing the content.
       account - The IRODSAccount identifying the connected user."}  
  UserGroupAO)

  
(defrecord MockUserAO [content-ref account]
  ^{:doc
    "This is a users accesser that is backed by a mutable content map.

     Parameters:
       content-ref - An atom containing the content.
       account - The IRODSAccount identifying the connected user."}  
  UserAO)


(defrecord MockQuotaAO [content-ref account]
  ^{:doc
    "This is a quotas accesser that is backed by a mutable content map.

     Parameters:
       content-ref - An atom containing the content.
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
         it should accept an IRODSAccount identifying the connected user."}
  IRODSAccessObjectFactory
  
  (getCollectionAO
    [_ acnt] 
    (collection-ctor acnt))

  (getCollectionAndDataObjectListAndSearchAO 
    [_ acnt] 
    (entry-list-ctor acnt))
   
  (getDataObjectAO 
    [_ acnt] 
    (data-obj-ctor acnt))

  (getIRODSFileSystemAO 
    [_ acnt] 
    (fs-ctor acnt))

  (getQuotaAO 
    [_ acnt]
    (quota-ctor acnt))

  (getUserAO 
    [_ acnt] 
    (user-ctor acnt))

  (getUserGroupAO 
    [_ acnt] 
    (group-ctor acnt)))


(defn mk-mock-ao-factory
  "Constructs a MockAOFactory with mutable repository content.
 
    Parameters:
      content-ref - An atom containing the content.
  
    Returns:
     It returns a MockProxy instance."
  [content-ref]
  (->MockAOFactory (partial ->MockFileSystemAO content-ref)
                   (partial ->MockEntryListAO content-ref)
                   (partial ->MockCollectionAO content-ref)
                   (partial ->MockDataObjectAO content-ref)
                   (partial ->MockGroupAO content-ref)
                   (partial ->MockUserAO content-ref)
                   (partial ->MockQuotaAO content-ref)))
  

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
         as its only argument."}
  ji/IRODSProxy
  
  (close 
    [_])

  (getIRODSAccessObjectFactory 
    [_] 
    (ao-factory-ctor))

  (getIRODSFileFactory 
    [_ acnt]
    (file-factory-ctor acnt)))
            

(defn mk-mock-proxy
  "Constructs a MockProxy with mutable repository content.
 
    Parameters:
      content-ref - An atom containing the content.
  
    Returns:
     It returns a MockProxy instance."
  [content-ref]
  (->MockProxy #(mk-mock-ao-factory content-ref) 
               (partial ->MockFileFactory content-ref)))
