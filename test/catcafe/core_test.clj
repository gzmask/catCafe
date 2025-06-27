(ns catcafe.core-test
  (:require [clojure.test :refer :all]
            [catcafe.core :refer [check-collision]]))

(deftest overlapping-rectangles
  (testing "Rectangles that overlap should collide"
    (is (true? (check-collision 0 0 50 50 25 25 50 50)))))

(deftest non-overlapping-rectangles
  (testing "Rectangles that do not overlap should not collide"
    (is (false? (check-collision 0 0 10 10 20 20 5 5)))))

