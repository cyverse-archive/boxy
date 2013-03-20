(defproject org.iplantc/boxy "0.1.2-SNAPSHOT"
  :description  "a mock of an iRODS repository to be used with clj-jargon"
  :url          "http://github.com/iPlantCollaborativeOpenSource/boxy"
  :license      {:url "file://LICENSE"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.irods.jargon/jargon-core "3.3.0"]
                 [slingshot "0.10.3"]
                 [org.iplantc/clojure-commons "1.4.1-SNAPSHOT"]]
  :repositories {"iplantCollaborative"
                 "http://projects.iplantcollaborative.org/archiva/repository/internal/",
                 
                 "renci.repository"
                 "http://ci-dev.renci.org/nexus/content/repositories/releases/"
                 
                 "renci.repository.snapshots"
                 "http://ci-dev.renci.org/nexus/content/repositories/snapshots/"})
