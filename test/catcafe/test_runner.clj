(ns catcafe.test-runner
  (:require [clojure.test :as t]
            catcafe.core-test))

(defn run
  "Run all tests." [_]
  (t/run-tests 'catcafe.core-test))

