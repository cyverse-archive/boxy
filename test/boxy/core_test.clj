(ns boxy.core-test
  (:use clojure.test
        boxy.core)
  (:require [slingshot.slingshot :as ss]
            [boxy.repo :as repo])
  (:import [org.irods.jargon.core.connection IRODSAccount] 
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
           [org.irods.jargon.core.pub.domain AvuData
                                             ObjStat$SpecColType]
           [org.irods.jargon.core.pub.io IRODSFile 
                                         IRODSFileFactory
                                         IRODSFileOutputStream]
           [org.irods.jargon.core.query MetaDataAndDomainData$MetadataDomain]))


(def ^{:private true} init-content 
  {:users                   #{["user1" "zone"] ["user2" "zone"]}
   :groups                  {["group" "zone"] #{["user1" "zone"] ["user2" "zone"]}}
   "/zone"                  {:type        :normal-dir
                             :creator     ["user1" "zone"]
                             :create-time 0
                             :modify-time 0
                             :acl         {}
                             :avus        {}}
   "/zone/home"             {:type        :normal-dir
                             :creator     ["user1" "zone"]
                             :create-time 0
                             :modify-time 0
                             :acl         {["user1" "zone"] :read 
                                           ["user2" "zone"] :read}
                             :avus        {}}
   "/zone/home/user1"       {:type        :normal-dir
                             :creator     ["user1" "zone"]
                             :create-time 0
                             :modify-time 0
                             :acl         {["user1" "zone"] :write}
                             :avus        {}}
   "/zone/home/user1/file"  {:type        :file
                             :create-time 1
                             :modify-time 2
                             :acl         {["user1" "zone"] :own}
                             :avus        {"attribute" ["value" "unit"]}
                             :content     ""}
   "/zone/home/user1/link"  {:type        :linked-dir
                             :create-time 3
                             :modify-time 3
                             :acl         {["user1" "zone"] :own}
                             :avus        {}}
   "/zone/home/user2"       {:type        :normal-dir
                             :creator     ["user1" "zone"]
                             :create-time 4
                             :modify-time 4
                             :acl         {["user2" "zone"] :write}
                             :avus        {}}
   "/zone/home/user2/file1" {:type        :file
                             :creator     ["user2" "zone"]
                             :create-time 5
                             :modify-time 6
                             :acl         {["user2" "zone"] :own}
                             :avus        {}
                             :content     ""}
   "/zone/home/user2/file2" {:type        :file
                             :creator     ["user2" "zone"]
                             :create-time 7
                             :modify-time 8
                             :acl         {["user2" "zone"] :own 
                                           ["user1" "zone"] :read}
                             :avus        {}
                             :content     ""}
   "/zone/home/user2/file3" {:type        :file
                             :creator     ["user2" "zone"]
                             :create-time 9
                             :modify-time 10
                             :acl         {["user2" "zone"] :own 
                                           ["user1" "zone"] :write}
                             :avus        {}
                             :content     ""}
   "/zone/home/user2/dir1"  {:type        :normal-dir
                             :creator     ["user2" "zone"]
                             :create-time 11
                             :modify-time 11
                             :acl         {["user2" "zone"] :own}
                             :avus        {}}
   "/zone/home/user2/dir2"  {:type        :normal-dir
                             :creator     ["user2" "zone"]
                             :create-time 11
                             :modify-time 11
                             :acl         {["user2" "zone"] :own}
                             :avus        {}}
   "/zone/home/user2/dir3"  {:type        :normal-dir
                             :creator     ["user2" "zone"]
                             :create-time 11
                             :modify-time 11
                             :acl         {["user2" "zone"] :own}
                             :avus        {}}
   "/zone/home/user2/dir4"  {:type        :normal-dir
                             :creator     ["user2" "zone"]
                             :create-time 11
                             :modify-time 11
                             :acl         {["user2" "zone"] :own}
                             :avus        {}}
   "/zone/home/user2/dir5"  {:type        :normal-dir
                             :creator     ["user2" "zone"]
                             :create-time 11
                             :modify-time 11
                             :acl         {["user2" "zone"] :own}
                             :avus        {}}
   "/zone/home/user2/dir6"  {:type        :normal-dir
                             :creator     ["user2" "zone"]
                             :create-time 11
                             :modify-time 11
                             :acl         {["user2" "zone"] :own}
                             :avus        {}}})


(def ^{:private true} account 
  (IRODSAccount. "localhost" 
                 1247 
                 "user1" 
                 "hackme"
                 "/zone/home/user1"
                 "zone"
                 "resource"))     


(defn- same-acl?
  [jargon-acl repo-acl]
  (let [conv-perm  (fn [jargon-perm] [(.getUserName jargon-perm)
                                      (.getUserZone jargon-perm)
                                      (.getFilePermissionEnum jargon-perm)])
        jargon-set (set (map conv-perm jargon-acl))
        repo-set   (set repo-acl)]
    (= jargon-set repo-set)))

                                                    
(deftest test-MockFile-createNewFile
  (testing "file doesn't already exist"
    (let [content-ref (atom init-content)
          path        "/zone/home/user1/new"
          result      (.createNewFile (->MockFile content-ref account path))]
      (is result)
      (is (repo/contains-entry? @content-ref path))))
  (testing "file already exists"
    (let [content-ref (atom init-content)
          result      (.createNewFile (->MockFile content-ref 
                                                  account  
                                                  "/zone/home/user1/file"))]
      (is (not result))
      (is (= init-content @content-ref)))))
  

(deftest test-MockFile-exists
  (is (.exists (->MockFile (atom init-content) 
                           account 
                           "/zone/home/user1/file"))))


(deftest test-MockFile-getAbsolutePath
  (is (= "/zone" 
         (.getAbsolutePath (->MockFile (atom init-content) account "/zone/")))))

  
(deftest test-MockFile-getParent
  (let [file (->MockFile (atom init-content) account "/zone/home")]
    (is (= "/zone" (.getParent file)))))


(deftest test-MockFile-initializeObjStatForFile
  (letfn [(get-type ([path] 
                     (.. (->MockFile (atom init-content) account path)
                       initializeObjStatForFile
                       getSpecColType)))] 
    (is (= ObjStat$SpecColType/NORMAL (get-type "/zone")))
    (is (= ObjStat$SpecColType/LINKED_COLL (get-type "/zone/home/user1/link")))
    (is (nil? (get-type "zone/home/user/file")))))


(deftest test-MockFile-isDirectory
  (let [content-ref (atom init-content)]
    (is (.isDirectory (->MockFile content-ref account "/zone")))
    (is (not (.isDirectory (->MockFile content-ref 
                                       account 
                                       "/zone/home/user1/file"))))))


(deftest test-MockFile-isFile
  (let [content-ref (atom init-content)]
    (is (.isFile (->MockFile content-ref account "/zone/home/user1/file")))
    (is (not (.isFile (->MockFile content-ref account "/zone"))))))


(deftest test-MockFileIOOperations-write
  (let [content-ref (atom nil)
        path        "/zone/home/user1/file"
        bytes       (.getBytes "content")
        ops         (->MockFileIOOperations content-ref 
                                            account 
                                            (->MockFile content-ref account path))]
    (testing "write 0 length"
      (reset! content-ref init-content)
      (let [write-count (.write ops 3 bytes 0 0)]
        (is (= 0 write-count))
        (is (= init-content @content-ref))))
    (testing "write 0 offset 2 length"
      (reset! content-ref init-content)
      (let [write-count (.write ops 3 bytes 0 1)]
        (is (= 1 write-count))
        (is (= "c" (:content (get @content-ref path))))))
    (testing "write 1 offset 2 length"
      (reset! content-ref init-content)
      (let [write-count (.write ops 3 bytes 1 2)]
        (is (= 2 write-count))
        (is (= " on" (:content (get @content-ref path))))))))
           

(deftest test-mock-file-factory-ctor
  (let [content-ref (atom init-content)
        factory     (mk-mock-file-factory content-ref account)]
    (is (instance? IRODSFile (.instanceIRODSFile factory "/zone/home/user1/file")))
    (is (instance? IRODSFileOutputStream 
                   (.instanceIRODSFileOutputStream factory 
                     (->MockFile content-ref account "/zone/home/user1/file"))))))


(deftest test-MockFileSystemAO-getListDir
  (let [content (atom init-content)
        ao      (->MockFileSystemAO content account false)]
    (testing "testing directory"
      (is (= ["home"] (.getListInDir ao (->MockFile content account "/zone")))))
    (testing "testing file"
      (is (= #{"file" "link"}
             (set (.getListInDir ao (->MockFile content account "/zone/home/user1/file"))))))
    (testing "missing entry"
      (let [thrown? (try 
                      (.getListInDir ao (->MockFile content account "/missing"))
                      false
                      (catch FileNotFoundException _ true))]
        (is thrown?)))
    (testing "oom"
      (let [ao-oom  (->MockFileSystemAO content account true)
            thrown? (try 
                      (.getListInDir ao-oom (->MockFile content account "/zone"))
                      false
                      (catch OutOfMemoryError _ true))]
        (is thrown?)))))
                        

(deftest test-MockEntryListAO-getCollectionAndDataObjectListingEntryAtGivenAbsolutePath
  (let [ao (->MockEntryListAO (atom init-content) account)]
    (testing "get collection"
      (let [path  "/zone"
            entry (.getCollectionAndDataObjectListingEntryAtGivenAbsolutePath ao path)]
        (is (.isCollection entry))
        (is (= path (.getFormattedAbsolutePath entry)))
        (is (= "user1" (.getOwnerName entry)))
        (is (= "zone" (.getOwnerZone entry)))
        (is (= 0 (.getTime (.getCreatedAt entry))))
        (is (= 0 (.getTime (.getModifiedAt entry))))))
    (testing "get data object"
      (let [path  "/zone/home/user1/file"
            entry (.getCollectionAndDataObjectListingEntryAtGivenAbsolutePath ao path)]
        (is (.isDataObject entry))))
    (testing "missing entry"
      (let [thrown? (ss/try+
                      (.getCollectionAndDataObjectListingEntryAtGivenAbsolutePath ao "/missing")
                      false
                      (catch Object _ true))]
        (is thrown?)))))
  
  
(deftest test-MockEntryListAO-listCollectionsUnderPathWithPermissions
  (let [ao (->MockEntryListAO (atom init-content) account)]
    (testing "list 1 normal collection"
      (let [colls (.listCollectionsUnderPathWithPermissions ao "/zone" 0)
            home  (first colls)
            perms (.getUserFilePermission home)]       
        (is (= 1 (count colls)))
        (is (= "/zone/home" (.getFormattedAbsolutePath home)))
        (is (.isCollection home))
        (is (= ObjStat$SpecColType/NORMAL (.getSpecColType home)))
        (is (= 2 (count perms)))
        (is (same-acl? perms [["user1" "zone" FilePermissionEnum/READ] 
                              ["user2" "zone" FilePermissionEnum/READ]])) 
        (is (= 1 (.getCount home)))
        (is (.isLastResult home))))
    (testing "list 1 linked collection"
      (let [colls (.listCollectionsUnderPathWithPermissions ao "/zone/home/user1" 0)
            link  (first colls)]       
        (is (= "/zone/home/user1/link" (.getFormattedAbsolutePath link)))
        (is (.isCollection link))
        (is (= ObjStat$SpecColType/LINKED_COLL (.getSpecColType link)))))
    (testing "paging"
      (let [page1 (.listCollectionsUnderPathWithPermissions ao "/zone/home/user2" 0)
            page2 (.listCollectionsUnderPathWithPermissions ao "/zone/home/user2" 5)]
        (is (= 5 (count page1)))
        (is (= 1 (.getCount (first page1))))
        (is (= 5 (.getCount (last page1))))
        (is (not (.isLastResult (last page1))))
        (is (= 1 (count page2)))
        (is (= 1 (.getCount (last page2))))
        (is (.isLastResult (last page2)))))))


(deftest test-MockEntryListAO-listDataObjectsUnderPathWithPermissions
  (let [ao    (->MockEntryListAO (atom init-content) account)
        files (.listDataObjectsUnderPathWithPermissions ao "/zone/home/user1" 0)
        file  (first files)]       
    (is (= 1 (count files)))
    (is (.isDataObject file))
    (is (= "/zone/home/user1/file" (.getFormattedAbsolutePath file)))))


(deftest test-MockCollectionAO-getPermissionForCollection
  (let [ao (->MockCollectionAO (atom init-content) account)]
    (is (= FilePermissionEnum/NONE
           (.getPermissionForCollection ao "/zone" "user1" "zone")))
    (is (= FilePermissionEnum/READ 
           (.getPermissionForCollection ao "/zone/home" "user1" "zone")))
    (is (= FilePermissionEnum/WRITE
           (.getPermissionForCollection ao "/zone/home/user1" "user1" "zone")))
    (is (= FilePermissionEnum/OWN 
           (.getPermissionForCollection ao "/zone/home/user1/link" "user1" "zone")))))
  

(deftest test-MockCollectionAO-listPermissionsForCollection
  (testing "multiple permissions are retrieved"
    (let [perms (.listPermissionsForCollection 
                  (->MockCollectionAO (atom init-content) account) 
                  "/zone/home")]
      (is (= 2 (count perms)))))
  (testing "structure of permission object"
    (let [perm (first (.listPermissionsForCollection 
                        (->MockCollectionAO (atom init-content) account) 
                        "/zone/home/user1"))]
      (is (= FilePermissionEnum/WRITE (.getFilePermissionEnum perm)))
      (is (= "user1" (.getUserName perm)))))
  (testing "missing collection"
    (let [ao (->MockCollectionAO (atom init-content) account)]
      (is (ss/try+
            (.listPermissionsForCollection ao "/missing")
            false
            (catch FileNotFoundException _
              true)))))
  (testing "fail if ends in /"
      (let [ao (->MockCollectionAO (atom init-content) account)]
      (is (ss/try+
            (.listPermissionsForCollection ao "/zone/")
            false
            (catch FileNotFoundException _
              true))))))
  
    
(deftest test-MockDataObjectAO-addAVUMetadata
  (let [content-ref (atom init-content)
        path        "/zone/home/user1/file"]
    (.addAVUMetadata (->MockDataObjectAO content-ref account)
                     path 
                     (AvuData. "a" "v" "u"))
    (is (-> @content-ref (repo/get-avus path) set (contains? ["a" "v" "u"])))))


(deftest test-MockDataObjectAO-findMetadataValuesForDataObject
  (let [metadata  (.findMetadataValuesForDataObject 
                    (->MockDataObjectAO (atom init-content) account) 
                    "/zone/home/user1/file")
        metadatum (first metadata)]
  (is (= 1 (count metadata)))
  (is (= "attribute" (.getAvuAttribute metadatum)))
  (is (= "unit" (.getAvuUnit metadatum)))
  (is (= "value" (.getAvuValue metadatum)))
  (is (= MetaDataAndDomainData$MetadataDomain/DATA (.getMetadataDomain metadatum)))))


(deftest test-MockDataObjectAO-getPermissionForDataObject
  (let [ao (->MockDataObjectAO (atom init-content) account)]
    (is (= FilePermissionEnum/NONE
           (.getPermissionForDataObject ao 
             "/zone/home/user2/file1" "user1" "zone")))
    (is (= FilePermissionEnum/READ 
           (.getPermissionForDataObject ao 
             "/zone/home/user2/file2" "user1" "zone")))
    (is (= FilePermissionEnum/WRITE
           (.getPermissionForDataObject ao 
             "/zone/home/user2/file3" "user1" "zone")))
    (is (= FilePermissionEnum/OWN 
           (.getPermissionForDataObject ao "/zone/home/user1/file" "user1" "zone")))))


(deftest test-MockDataObject-listPermissionsForDataObject
  (testing "multiple permissions are retrieved"
    (let [perms (.listPermissionsForDataObject 
                  (->MockDataObjectAO (atom init-content) account) 
                  "/zone/home/user2/file2")]
      (is (= 2 (count perms)))))
  (testing "structure of permission object"
    (let [perm (first (.listPermissionsForDataObject 
                        (->MockDataObjectAO (atom init-content) account) 
                        "/zone/home/user1/file"))]
      (is (= FilePermissionEnum/OWN (.getFilePermissionEnum perm)))
      (is (= "user1" (.getUserName perm)))))
  (testing "missing data object"
    (let [ao (->MockDataObjectAO (atom init-content) account)]
      (is (ss/try+
            (.listPermissionsForDataObject ao "/missing")
            false
            (catch FileNotFoundException _
              true))))))
 
  
(deftest test-MockGroupAO-findUserGroupsForUser
  (let [groups (.findUserGroupsForUser (->MockGroupAO (atom init-content) 
                                                      account) 
                                       "user1")
        group  (first groups)]
    (is (= 1 (count groups)))
    (is (= "group" (.getUserGroupName group)))
    (is (= "zone" (.getZone group)))))
  

(deftest test-MockUserAO-findByName
  (let [ao (->MockUserAO (atom init-content) account)]
    (testing "known user"
      (let [user (.findByName ao "user1")]
        (is (= "user1" (.getName user)))
        (is (= "zone" (.getZone user)))))
    (testing "unknown user"
      (let [thrown? (try
                      (.findByName ao "unknown")
                      false
                      (catch DataNotFoundException _
                        true))]
        (is thrown?)))))
    
  
(deftest test-ao-factory-ctor
  (let [factory (mk-mock-ao-factory (atom init-content))]
    (is (instance? CollectionAO (.getCollectionAO factory account)))
    (is (instance? CollectionAndDataObjectListAndSearchAO 
                   (.getCollectionAndDataObjectListAndSearchAO factory account)))
    (is (instance? DataObjectAO (.getDataObjectAO factory account)))
    (is (instance? IRODSFileSystemAO (.getIRODSFileSystemAO factory account)))
    (is (instance? QuotaAO (.getQuotaAO factory account)))
    (is (instance? UserAO (.getUserAO factory account)))
    (is (instance? UserGroupAO (.getUserGroupAO factory account)))))


(deftest test-proxy-ctor
  (let [mock (mk-mock-proxy (atom init-content))]
    (is (instance? IRODSAccessObjectFactory (.getIRODSAccessObjectFactory mock)))
    (is (instance? IRODSFileFactory (.getIRODSFileFactory mock nil)))))
