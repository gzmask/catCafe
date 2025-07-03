(ns catcafe.core-test
  (:require [clojure.test :refer :all]
            [catcafe.core :refer [check-collision]]))

(deftest overlapping-rectangles
  (testing "Rectangles that overlap should collide"
    (is (true? (check-collision {:x 0 :y 0 :width 50 :height 50}
                                {:x 25 :y 25 :width 50 :height 50})))))

(deftest non-overlapping-rectangles
  (testing "Rectangles that do not overlap should not collide"
    (is (false? (check-collision {:x 0 :y 0 :width 10 :height 10}
                                 {:x 20 :y 20 :width 5 :height 5})))))

