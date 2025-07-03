(ns catcafe.systems-test
  (:require [clojure.test :refer :all]
            [catcafe.systems :as systems]
            [catcafe.entity :as entity]
            [catcafe.components :as c]))

(deftest movement-updates-position
  (reset! entity/registry {})
  (reset! entity/next-id 0)
  (let [id (entity/create-entity {:position (c/position 0 0)
                                  :velocity (c/velocity 10 0)})]
    (systems/movement-system 1.0)
    (is (= 10 (:x (:position (@entity/registry id)))))))

(deftest animation-increments-time
  (reset! entity/registry {})
  (reset! entity/next-id 0)
  (let [id (entity/create-entity {:animation (c/animation nil nil)})]
    (systems/animation-system 0.5)
    (is (= 0.5 (-> (@entity/registry id) :animation :time)))))
