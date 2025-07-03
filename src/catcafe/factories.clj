(ns catcafe.factories
  (:require [catcafe.entity :as e]
            [catcafe.components :as c]
            [catcafe.core :refer [create-walking-animation-right
                                   create-walking-animation-left
                                   player-visual-width
                                   player-visual-height]])
  (:import (com.badlogic.gdx.graphics Texture Sprite)))

(defn create-player
  []
  (let [standing-texture (Texture. "images/ysabelWalkingRight1.png")
        standing-right (doto (Sprite. standing-texture)
                         (.setSize player-visual-width player-visual-height))
        standing-left (doto (Sprite. standing-texture)
                        (.setSize player-visual-width player-visual-height)
                        (.flip true false))
        walk-right (create-walking-animation-right)
        walk-left (create-walking-animation-left)]
    (e/create-entity
      {:position (c/position 400 100)
       :velocity (c/velocity 0 0)
       :sprite (c/sprite standing-right standing-left
                         player-visual-width player-visual-height)
       :animation (c/animation walk-right walk-left)
       :input (c/input)})))

(defn create-npc
  []
  (let [standing-texture (Texture. "images/maiaWalkingRight1.png")
        standing-right (doto (Sprite. standing-texture)
                         (.setSize player-visual-width player-visual-height))
        standing-left (doto (Sprite. standing-texture)
                        (.setSize player-visual-width player-visual-height)
                        (.flip true false))
        walk-right (catcafe.core/create-maia-walking-animation-right)
        walk-left (catcafe.core/create-maia-walking-animation-left)]
    (e/create-entity
      {:position (c/position 200 100)
       :velocity (c/velocity 0 0)
       :sprite (c/sprite standing-right standing-left
                         player-visual-width player-visual-height)
       :animation (c/animation walk-right walk-left)})))
