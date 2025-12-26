(ns catcafe.desktop-launcher
  (:require [catcafe.core :as core])
  (:import [com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application Lwjgl3ApplicationConfiguration]))

(defn- start-nrepl-server! []
  (try
    (require '[nrepl.server :as nrepl])
    (let [server ((resolve 'nrepl.server/start-server) :port 7889)]
      (println "nREPL server started on port 7889 (game REPL)")
      (println "Connect via clojure-mcp to port 7889 to control the running game!")
      server)
    (catch Exception e
      (println "Could not start nREPL server:" (.getMessage e))
      nil)))

(defn -main []
  ;; Start the game
  (let [_nrepl-app (future (start-nrepl-server!))
        config (doto (Lwjgl3ApplicationConfiguration.)
                 (.setTitle "Cat Cafe Game")
                 (.setWindowedMode 800 600))]
    (Lwjgl3Application. (core/create-game) config)))

