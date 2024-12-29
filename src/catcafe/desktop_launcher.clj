(ns catcafe.desktop-launcher
  (:require [catcafe.core :as core])
  (:import [com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application Lwjgl3ApplicationConfiguration]))

(defn -main []
  (let [config (doto (Lwjgl3ApplicationConfiguration.)
                 (.setTitle "My Game")
                 (.setWindowedMode 800 600))]
    (Lwjgl3Application. (core/create-game) config)))

