(defproject org.iplantc/boxy "0.1.2-SNAPSHOT"
  :description  "a mock of an iRODS repository to be used with clj-jargon"
  :license {:name "BSD"
            :url "http://iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt"}
  :url "http://github.com/iPlantCollaborativeOpenSource/boxy"
  :scm {:connection "scm:git:git@github.com:iPlantCollaborativeOpenSource/kameleon.git"
        :developerConnection "scm:git:git@github.com:iPlantCollaborativeOpenSource/kameleon.git"
        :url "git@github.com:iPlantCollaborativeOpenSource/kameleon.git"}
  :pom-addition [:developers
                 [:developer
                  [:url "https://github.com/orgs/iPlantCollaborativeOpenSource/teams/iplant-devs"]]]
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.irods.jargon/jargon-core "3.3.0"]
                 [slingshot "0.10.3"]
                 [org.iplantc/clojure-commons "1.4.1-SNAPSHOT"]]
  :repositories [["sonatype-nexus-snapshots"
                  {:url "https://oss.sonatype.org/content/repositories/snapshots"}]
                 ["sonatype-nexus-staging"
                  {:url "https://oss.sonatype.org/service/local/staging/deploy/maven2/"}]
                 ["renci.repository.releases"
                  {:url "http://ci-dev.renci.org/nexus/content/repositories/releases/"}]
                 ["renci.repository.snapshots"
                  {:url "http://ci-dev.renci.org/nexus/content/repositories/snapshots/"}]])
