(ns boxy.fake-repos
  (:use boxy.core))

(def simple-repo
  "A fairly simple repo. No shared files, two users, two admins."
  {:users #{"test-user" "test-user1" "test-admin" "test-admin1"}

   :groups {["rodsadmin" "iplant"] #{"test-admin" "test-admin1"}
            ["public" "iplant"]    #{"test-user" "test-user1"}}

   "/iplant"                       {:type :dir
                                    :acl {"rodsadmin" :own}
                                    :avus {}}

   "/iplant/home"                  {:type :dir
                                    :acl {"rodsadmin" :own
                                          "test-user" :read
                                          "test-user1" :read}
                                    :avus {}}

   "/iplant/home/test-user"        {:type :dir
                                    :acl {"rodsadmin" :own
                                          "test-user" :own}
                                    :avus {}}

   "/iplant/home/test-user/file1"  {:type :file
                                    :acl {"rodsadmin" :own
                                          "test-user" :own}
                                    :avus {}}

   "/iplant/home/test-user1"        {:type :dir
                                     :acl {"rodsadmin" :own
                                           "test-user1" :own}
                                     :avus {}}

   "/iplant/home/test-user1/file1" {:type :dir
                                    :acl {"rodsadmin" :own
                                          "test-user1" :own}
                                    :avus {}}})

;;;Pass this in as the :proxy-ctr value for clj-jargon's (init).
(def simple-proxy #(mk-mock-proxy (atom simple-repo)))
