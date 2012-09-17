(ns boxy.jargon-if
  "This namespace defines the interface that should have been defined in Jargon.")


(defprotocol IRODSProxy
  "These are the org.irods.jargon.core.pub.IRODSFileSystem methods that need to 
    be implemented by a proxy for a mock iRODS repository so that this library 
    can use it instead of an actual iRODS repository.  These methods must be 
    semantically equivalent to the IRODSFileSystem methods with the same name.

    NOTE:  This is incomplete.  Only the method currently used by clj-jargon
    have been defined."
  (close [_])
  (getIRODSAccessObjectFactory [_])
  (getIRODSFileFactory [_ irods-account]))
