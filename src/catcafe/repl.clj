(ns catcafe.repl
  "REPL helper functions for interactive development and player control."
  (:require [catcafe.core :as core]))

;; =============================================================================
;; REPL Helper Functions for Interactive Development
;; =============================================================================

(defn get-player-position
  "Get current player position for debugging."
  []
  (when-let [player-id (:player-id @core/game-state)]
    (core/get-component player-id :position)))

(defn get-maia-position
  "Get current Maia (NPC) position for debugging."
  []
  (when-let [maia-id (:maia-id @core/game-state)]
    (core/get-component maia-id :position)))

(defn simulate-key-press!
  "Simulate pressing a key (:left, :right, :up, :down) for the player."
  [key]
  (when-let [player-id (:player-id @core/game-state)]
    (let [input-comp (core/get-component player-id :input-comp)
          new-active-keys (conj (:active-keys input-comp) key)
          new-input-comp (core/->InputComp (:keys input-comp) new-active-keys)]
      (core/add-component! player-id :input-comp new-input-comp)
      (println "Simulated key press:" key))))

(defn simulate-key-release!
  "Simulate releasing a key (:left, :right, :up, :down) for the player."
  [key]
  (when-let [player-id (:player-id @core/game-state)]
    (let [input-comp (core/get-component player-id :input-comp)
          new-active-keys (disj (:active-keys input-comp) key)
          new-input-comp (core/->InputComp (:keys input-comp) new-active-keys)]
      (core/add-component! player-id :input-comp new-input-comp)
      (println "Simulated key release:" key))))

(defn simulate-move!
  "Simulate movement in a direction (:left, :right, :up, :down) for specified duration in seconds."
  [direction duration-seconds]
  (simulate-key-press! direction)
  (future
    (Thread/sleep (* duration-seconds 1000))
    (simulate-key-release! direction))
  (println "Simulated movement:" direction "for" duration-seconds "seconds"))

(defn stop-all-movement!
  "Stop all simulated player movement by clearing active keys."
  []
  (when-let [player-id (:player-id @core/game-state)]
    (let [input-comp (core/get-component player-id :input-comp)
          new-input-comp (core/->InputComp (:keys input-comp) #{})]
      (core/add-component! player-id :input-comp new-input-comp)
      (println "Stopped all simulated movement"))))

(defn get-active-keys
  "Get currently active simulated keys for the player."
  []
  (when-let [player-id (:player-id @core/game-state)]
    (let [input-comp (core/get-component player-id :input-comp)]
      (:active-keys input-comp))))

(defn debug-entities
  "Inspect current ECS state - entities and their components."
  []
  (println "=== ECS Debug Information ===")
  (println "Total entities:" (count @core/entities))
  (println "Entity IDs:" @core/entities)
  (println "\n=== World Components ===")
  (doseq [[entity-id component-map] @core/world]
    (println "Entity" entity-id ":")
    (doseq [[component-type component-data] component-map]
      (println "  " component-type ":" component-data)))
  (println "\n=== Game State ===")
  (let [state @core/game-state]
    (println "Player ID:" (:player-id state))
    (println "Maia ID:" (:maia-id state))
    (println "Camera:" (type (:camera state)))
    (println "Batch:" (type (:batch state)))))

;; Note: REPL-based game window control functions (start-game!, stop-game!,
;; reload-game!) have been removed as they are not compatible with macOS
;; -XstartOnFirstThread requirement. Use clj -M:run-game-with-repl instead.

(defn list-repl-commands
  "Show available REPL helper commands."
  []
  (println "=== Cat Cafe Game REPL Commands ===")
  (println "Note: Start the game with 'clj -M:run-game-with-repl' to use these commands")
  (println "")
  (println "(debug-entities)    - Show ECS state information")
  (println "(get-player-position) - Get player coordinates")
  (println "(get-maia-position)   - Get Maia coordinates")
  (println "")
  (println "=== Player Control Commands ===")
  (println "(simulate-key-press! :left)   - Press and hold left key")
  (println "(simulate-key-release! :left) - Release left key")
  (println "(simulate-move! :right 2)     - Move right for 2 seconds")
  (println "(stop-all-movement!)          - Stop all movement")
  (println "(get-active-keys)             - Show currently pressed keys")
  (println "(list-repl-commands)          - Show this help"))

(comment

  (list-repl-commands)
  (get-player-position)
  (get-maia-position)
  (simulate-key-press! :left)
  (simulate-move! :right 1)
  (get-active-keys)
  (stop-all-movement!)


  )
