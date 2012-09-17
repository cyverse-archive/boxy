(ns boxy.test-runner
  (:use clojure.test)
  (:require [boxy.core-test]))

(run-tests 'boxy.core-test)
