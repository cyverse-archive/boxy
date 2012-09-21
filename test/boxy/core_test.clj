(ns boxy.core-test
  (:use clojure.test
        boxy.core)
  (:require [boxy.repo :as r])
  (:import [org.irods.jargon.core.connection IRODSAccount] 
           [org.irods.jargon.core.protovalues FilePermissionEnum]
           [org.irods.jargon.core.pub CollectionAO
                                      CollectionAndDataObjectListAndSearchAO
                                      DataObjectAO
                                      IRODSAccessObjectFactory
                                      IRODSFileSystemAO
                                      QuotaAO
                                      UserAO
                                      UserGroupAO]
           [org.irods.jargon.core.pub.domain AvuData]
           [org.irods.jargon.core.pub.io IRODSFile 
                                         IRODSFileFactory
                                         IRODSFileOutputStream]
           [org.irods.jargon.core.query MetaDataAndDomainData$MetadataDomain]))


(def ^{:private true} init-content 
  {:groups                {["group" "zone"] #{"user"}}
   "/zone"                {:type :dir
                           :acl  {}
                           :avus {}}
   "/zone/home"           {:type :dir
                           :acl  {"user" :read}
                           :avus {}}
   "/zone/home/user"      {:type :dir
                           :acl  {"user" :write}
                           :avus {}}
   "/zone/home/user/file" {:type    :file
                           :acl     {"user" :own}
                           :avus    {"attribute" ["value" "unit"]}
                           :content ""}})


(def ^{:private true} account 
  (IRODSAccount. "localhost" 
                 1247 
                 "user" 
                 "hackme"
                 "/zone/home/user"
                 "zone"
                 "resource"))     


(deftest test-MockFile-createNewFile
  (testing "file doesn't already exist"
    (let [content-ref (atom init-content)
          path        "/zone/home/user/new"
          result      (.createNewFile (->MockFile content-ref account path))]
      (is result)
      (is (r/contains-entry? @content-ref path))))
  (testing "file already exists"
    (let [content-ref (atom init-content)
          result      (.createNewFile (->MockFile content-ref 
                                                  account  
                                                  "/zone/home/user/file"))]
      (is (not result))
      (is (= init-content @content-ref)))))
  

(deftest test-MockFile-exists
  (is (.exists (->MockFile (atom init-content) 
                           account 
                           "/zone/home/user/file"))))


(deftest test-MockFile-isDirectory
  (let [content-ref (atom init-content)]
    (is (.isDirectory (->MockFile content-ref account "/zone")))
    (is (not (.isDirectory (->MockFile content-ref account "/zone/home/user/file"))))))


(deftest test-MockFile-isFile
  (let [content-ref (atom init-content)]
    (is (.isFile (->MockFile content-ref account "/zone/home/user/file")))
    (is (not (.isFile (->MockFile content-ref account "/zone"))))))


(deftest test-MockFileIOOperations-write
  (let [content-ref (atom nil)
        path        "/zone/home/user/file"
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
    (is (instance? IRODSFile (.instanceIRODSFile factory "/zone/home/user/file")))
    (is (instance? IRODSFileOutputStream 
                   (.instanceIRODSFileOutputStream factory 
                     (->MockFile content-ref account "/zone/home/user/file"))))))


(deftest test-MockCollectionAO-getPermissionForCollection
  (let [ao (->MockCollectionAO (atom init-content) account)]
    (is (= FilePermissionEnum/NONE
           (.getPermissionForCollection ao "/zone" "user" "zone")))
    (is (= FilePermissionEnum/READ 
           (.getPermissionForCollection ao "/zone/home" "user" "zone")))
    (is (= FilePermissionEnum/WRITE
           (.getPermissionForCollection ao "/zone/home/user" "user" "zone")))
    (is (= FilePermissionEnum/OWN 
           (.getPermissionForCollection ao "/zone/home/user/file" "user" "zone")))))
    
  
(deftest test-MockDataObjectAO-addAVUMetadata
  (let [content-ref (atom init-content)
        path        "/zone/home/user/file"]
    (.addAVUMetadata (->MockDataObjectAO content-ref account)
                     path 
                     (AvuData. "a" "v" "u"))
    (is (-> @content-ref (r/get-avus path) set (contains? ["a" "v" "u"])))))


(deftest test-MockDataObjectAO-findMetadataValuesForDataObject
  (let [metadata  (.findMetadataValuesForDataObject 
                    (->MockDataObjectAO (atom init-content) account) 
                    "/zone/home/user/file")
        metadatum (first metadata)]
  (is (= 1 (count metadata)))
  (is (= "attribute" (.getAvuAttribute metadatum)))
  (is (= "unit" (.getAvuUnit metadatum)))
  (is (= "value" (.getAvuValue metadatum)))
  (is (= MetaDataAndDomainData$MetadataDomain/DATA (.getMetadataDomain metadatum)))))


(deftest test-MockGroupAO-findUserGroupsForUser
  (let [groups (.findUserGroupsForUser (->MockGroupAO (atom init-content) 
                                                      account) 
                                       "user")
        group  (first groups)]
    (is (= 1 (count groups)))
    (is (= "group" (.getUserGroupName group)))
    (is (= "zone" (.getZone group)))))
  

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
