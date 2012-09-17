# boxy

This is a library intended to back the clj-jargon with a configurable mock up of
an iRODS repository for unit testing purposes.

_Currently this is just a stub implementation.  It will be extended as needed 
for unit testing on iPlant projects._


## iRODS Proxy

Unfortunately, the main entry point into Jargon library is through the concrete
class org.irods.jargon.core.pub.IRODSFileSystem.  To work around this lack of
interface, the irods-mock.jargon-if/IRODSProxy protocol was created.  Through 
duck typing, an instance of any record that extends this protocol may be used in 
place of an instance of IRODSFileSystem.


## Usage

Normally, the client would populate a content map* with the test data.  The 
client would then pass this map to irods-mock.core/mk-mock-proxy function to
construct an IRODSProxy instance.  Under the hood, it constructs a MockProxy
object with appropriate constructors.

If the client would rather use something other than a content map to model an
iRODS repository, the MockProxy constructor may be used directly, passing in
constructors for the custom components.

* The structure of the content map has not been defined yet.
