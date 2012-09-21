(ns boxy.test-runner
  (:use clojure.test)
  (:require boxy.core-test
            boxy.repo-test))


(run-tests 'boxy.core-test
           'boxy.repo-test)
