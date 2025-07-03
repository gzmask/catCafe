(ns catcafe.components)

(defrecord Position [x y])
(defrecord Velocity [dx dy])
(defrecord Sprite [standing-right standing-left width height])
(defrecord Animation [walk-right walk-left time])
(defrecord Input [left? right? up? down?])

(defn position [x y]
  (->Position x y))

(defn velocity [dx dy]
  (->Velocity dx dy))

(defn sprite [standing-right standing-left width height]
  (->Sprite standing-right standing-left width height))

(defn animation [walk-right walk-left]
  (->Animation walk-right walk-left 0))

(defn input []
  (->Input false false false false))
