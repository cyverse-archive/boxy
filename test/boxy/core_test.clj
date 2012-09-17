(ns boxy.core-test
  (:use clojure.test
        boxy.core)
  (:import [org.irods.jargon.core.pub CollectionAO
                                      CollectionAndDataObjectListAndSearchAO
                                      DataObjectAO
                                      IRODSAccessObjectFactory
                                      IRODSFileSystemAO
                                      QuotaAO
                                      UserAO
                                      UserGroupAO]
            [org.irods.jargon.core.pub.io IRODSFileFactory]))


(deftest test-ao-factory-ctor
  (let [factory (mk-mock-ao-factory nil)]
    (is (instance? CollectionAO (.getCollectionAO factory nil)))
    (is (instance? CollectionAndDataObjectListAndSearchAO 
                   (.getCollectionAndDataObjectListAndSearchAO factory nil)))
    (is (instance? DataObjectAO (.getDataObjectAO factory nil)))
    (is (instance? IRODSFileSystemAO (.getIRODSFileSystemAO factory nil)))
    (is (instance? QuotaAO (.getQuotaAO factory nil)))
    (is (instance? UserAO (.getUserAO factory nil)))
    (is (instance? UserGroupAO (.getUserGroupAO factory nil)))))


(deftest test-proxy-ctor
  (let [mock (mk-mock-proxy nil)]
    (is (instance? IRODSAccessObjectFactory (.getIRODSAccessObjectFactory mock)))
    (is (instance? IRODSFileFactory (.getIRODSFileFactory mock nil)))))
