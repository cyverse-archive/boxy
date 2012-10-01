(defproject org.iplantc/boxy "0.1.0-SNAPSHOT"
  :description "a mock of an iRODS repository to be used with clj-jargon"
  :url "http://github.com/iPlantCollaborativeOpenSource/boxy"
  :license {:url "file://LICENSE"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.iplantc/clojure-commons "1.1.0-SNAPSHOT"]
                 [org.irods.jargon/jargon-core "3.2.0-SNAPSHOT"]]
  :repositories {"iplantCollaborative"
                 "http://projects.iplantcollaborative.org/archiva/repository/internal/",
                 
                 "renci.repository"
                 "http://ci-dev.renci.org/nexus/content/repositories/snapshots/"})
