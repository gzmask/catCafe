(ns catcafe.systems
  (:require [catcafe.entity :as e]
            [catcafe.components :as c]
            [clojure.set :as set])
  (:import (com.badlogic.gdx Gdx)
           (com.badlogic.gdx Input$Keys)))

(defn input-system
  [delta]
  (doseq [[id entity] @e/registry]
    (when-let [input (:input entity)]
      (let [left? (.isKeyPressed Gdx/input Input$Keys/LEFT)
            right? (.isKeyPressed Gdx/input Input$Keys/RIGHT)
            up? (.isKeyPressed Gdx/input Input$Keys/UP)
            down? (.isKeyPressed Gdx/input Input$Keys/DOWN)
            speed catcafe.core/player-speed
            vx (cond left? (- speed) right? speed :else 0)
            vy (cond down? (- speed) up? speed :else 0)]
        (e/update-entity id assoc
                         :input (assoc input :left? left? :right? right? :up? up? :down? down?)
                         :velocity (c/velocity vx vy))))))

(defn movement-system
  [delta]
  (doseq [[id entity] @e/registry]
    (when-let [pos (:position entity)]
      (let [vel (:velocity entity)
            dx (* (:dx vel) delta)
            dy (* (:dy vel) delta)
            new-x (+ (:x pos) dx)
            new-y (+ (:y pos) dy)
            new-x (-> new-x (max catcafe.core/floor-x-min) (min catcafe.core/floor-x-max))
            new-y (-> new-y (max catcafe.core/floor-y-min) (min catcafe.core/floor-y-max))
            new-pos (c/position new-x new-y)]
        (e/update-entity id assoc :position new-pos)))))

(defn animation-system
  [delta]
  (doseq [[id entity] @e/registry]
    (when-let [anim (:animation entity)]
      (e/update-entity id update-in [:animation :time] + delta))))

(defn npc-behavior-system
  [delta]
  ;; Placeholder for NPC behavior
  )

(defn render-system
  [batch]
  (doseq [[_ {:keys [position sprite animation input]}] @e/registry]
    (when (and position sprite)
      (let [{:keys [x y]} position
            {:keys [standing-right standing-left width height]} sprite
            facing-right? (if input (not (:left? input)) true)
            current-standing (if facing-right? standing-right standing-left)]
        (.setPosition current-standing (float x) (float y))
        (.draw current-standing batch)))))
