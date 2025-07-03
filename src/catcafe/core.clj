(ns catcafe.core
  (:require [catcafe.entity :as entity]
            [catcafe.factories :as factories]
            [catcafe.systems :as systems])
  (:import
    (com.badlogic.gdx
      Game
      Gdx
      Graphics
      Input$Keys)
    (com.badlogic.gdx.graphics
      GL20
      OrthographicCamera
      Texture)
    (com.badlogic.gdx.graphics.g2d
      Animation
      Sprite
      SpriteBatch
      TextureRegion)))


(def ^:const player-speed 200)
(def ^:const animation-frame-duration 0.1)
(def ^:const floor-y-min 0)         ;; Bottom of the screen
(def ^:const floor-y-max 180)       ;; Approximate floor level based on the image
(def ^:const floor-x-min 0)         ;; Left edge of the screen
(def ^:const floor-x-max 800)       ;; Right edge of the screen
(def ^:const player-visual-width 121.5)
(def ^:const player-visual-height 175.5)
(def ^:const player-collision-size 81)
(def ^:const npc-speed 100)
(def ^:const npc-visual-width 121.5)
(def ^:const npc-visual-height 175.5)
(def ^:const npc-direction-change-interval 3.0) ;; Change direction every 3 seconds
(def game-state (atom nil))


(defn create-initial-state
  []
  {:player-x 400
   :player-y 100
   :animation-time 0
   :is-moving false
   :facing-right true
   :player-id nil
   ;; Maia NPC state
   :maia-x 200
   :maia-y 100
   :maia-animation-time 0
   :maia-facing-right true
   :maia-direction-timer 0
   :maia-moving-right true})


(defn create-walking-animation-right
  []
  (let [walking1 (Texture. "images/ysabelWalkingRight1.png")
        walking2 (Texture. "images/ysabelWalkingRight2.png")
        frames (into-array TextureRegion
                           [(TextureRegion. walking1)
                            (TextureRegion. walking2)])]
    (Animation. (float animation-frame-duration) frames)))

(defn create-walking-animation-left
  []
  (let [walking1 (Texture. "images/ysabelWalkingRight1.png")
        walking2 (Texture. "images/ysabelWalkingRight2.png")
        region1 (TextureRegion. walking1)
        region2 (TextureRegion. walking2)
        _ (.flip region1 true false)
        _ (.flip region2 true false)
        frames (into-array TextureRegion [region1 region2])]
    (Animation. (float animation-frame-duration) frames)))

(defn create-maia-walking-animation-right
  []
  (let [walking1 (Texture. "images/maiaWalkingRight1.png")
        walking2 (Texture. "images/maiaWalkingRight2.png")
        frames (into-array TextureRegion
                           [(TextureRegion. walking1)
                            (TextureRegion. walking2)])]
    (Animation. (float animation-frame-duration) frames)))

(defn create-maia-walking-animation-left
  []
  (let [walking1 (Texture. "images/maiaWalkingRight1.png")
        walking2 (Texture. "images/maiaWalkingRight2.png")
        region1 (TextureRegion. walking1)
        region2 (TextureRegion. walking2)
        _ (.flip region1 true false)
        _ (.flip region2 true false)
        frames (into-array TextureRegion [region1 region2])]
    (Animation. (float animation-frame-duration) frames)))

;; Utility to dispose all textures associated with an animation
(defn dispose-animation-textures
  [^Animation animation]
  (doseq [^TextureRegion frame (.getKeyFrames animation)]
    (.dispose (.getTexture frame))))


(defn check-collision
  "Collision check using component maps with :x :y :width :height keys."
  [a b]
  (let [{x1 :x y1 :y w1 :width h1 :height} a
        {x2 :x y2 :y w2 :width h2 :height} b]
    (not (or (< (+ x1 w1) x2)
             (> x1 (+ x2 w2))
             (< (+ y1 h1) y2)
             (> y1 (+ y2 h2))))))


(defn create-game
  []
  (proxy [Game] []
    (create
      []
      (let [camera (OrthographicCamera.)
            batch (SpriteBatch.)

            ;; Create Maia standing textures and sprites
            maia-standing-texture (Texture. "images/maiaWalkingRight1.png")
            maia-standing-sprite-right (doto (Sprite. maia-standing-texture)
                                         (.setSize npc-visual-width npc-visual-height))
            maia-standing-sprite-left (doto (Sprite. maia-standing-texture)
                                        (.setSize npc-visual-width npc-visual-height)
                                        (.flip true false))

            ;; Create Maia walking animations
            maia-walking-animation-right (create-maia-walking-animation-right)
            maia-walking-animation-left (create-maia-walking-animation-left)

            hallway-bg-texture (Texture. "images/hallway_bg.png")
            player-id (factories/create-player)]
        (.setToOrtho camera false 800 600)
        (reset! game-state
                (assoc (create-initial-state)
                       :camera camera
                       :batch batch
                       :player-id player-id
                       :maia-standing-sprite-right maia-standing-sprite-right
                       :maia-standing-sprite-left maia-standing-sprite-left
                       :maia-walking-animation-right maia-walking-animation-right
                       :maia-walking-animation-left maia-walking-animation-left
                       :hallway-bg-texture hallway-bg-texture))))

    (render
      []
      (let [{:keys [camera batch hallway-bg-texture
                    maia-x maia-y maia-facing-right maia-animation-time
                    maia-walking-animation-right maia-walking-animation-left
                    maia-standing-sprite-right maia-standing-sprite-left]} @game-state
            delta (.getDeltaTime Gdx/graphics)]

        ;; Clear screen
        (.glClearColor Gdx/gl 0 0 0.2 1)
        (.glClear Gdx/gl GL20/GL_COLOR_BUFFER_BIT)

        ;; Update camera
        (.update camera)
        (.setProjectionMatrix batch (.combined camera))

        ;; Run ECS systems for the player entity
        (systems/input-system delta)
        (systems/movement-system delta)
        (systems/animation-system delta)

        ;; Update Maia NPC
        (let [{:keys [maia-x maia-y maia-direction-timer maia-moving-right maia-animation-time]} @game-state
              new-maia-direction-timer (+ maia-direction-timer delta)
              
              ;; Change direction periodically
              [new-maia-moving-right new-timer] (if (>= new-maia-direction-timer npc-direction-change-interval)
                                                  [(not maia-moving-right) 0]
                                                  [maia-moving-right new-maia-direction-timer])
              
              ;; Calculate new Maia position
              new-maia-x (if new-maia-moving-right
                           (min (- floor-x-max player-collision-size) (+ maia-x (* npc-speed delta)))
                           (max floor-x-min (- maia-x (* npc-speed delta))))
              
              new-maia-animation-time (+ maia-animation-time delta)]
          
          (swap! game-state assoc
                 :maia-x new-maia-x
                 :maia-direction-timer new-timer
                 :maia-moving-right new-maia-moving-right
                 :maia-facing-right new-maia-moving-right
                 :maia-animation-time new-maia-animation-time))

        ;; Draw
        (.begin batch)
        ;; Draw background first
        (.draw batch hallway-bg-texture
               (float 0) (float 0)
               (float 800) (float 600))  ;; Use the window size

        ;; Draw Maia NPC (before player so player appears in front)
        (let [{:keys [maia-x maia-y maia-facing-right maia-animation-time
                      maia-walking-animation-right maia-walking-animation-left
                      maia-standing-sprite-right maia-standing-sprite-left]} @game-state
              maia-current-animation (if maia-facing-right
                                       maia-walking-animation-right
                                       maia-walking-animation-left)
              maia-current-standing (if maia-facing-right
                                      maia-standing-sprite-right
                                      maia-standing-sprite-left)
              maia-current-frame (.getKeyFrame maia-current-animation maia-animation-time true)]
          (.draw batch maia-current-frame
                 (float maia-x) (float maia-y)
                 (float npc-visual-width) (float npc-visual-height)))

        ;; Draw player entity via ECS render system
        (systems/render-system batch)

        (.end batch)))

    (dispose
      []
      (let [{:keys [batch
                    maia-standing-sprite-right maia-walking-animation-right maia-walking-animation-left
                    hallway-bg-texture]} @game-state]
        (.dispose batch)

        ;; Dispose all entity resources
        (doseq [[_ {:keys [sprite animation]}] @entity/registry]
          (when sprite
            (.dispose (.getTexture (:standing-right sprite))))
          (when animation
            (dispose-animation-textures (:walk-right animation))
            (dispose-animation-textures (:walk-left animation))))

        ;; Dispose Maia textures
        (.dispose (.getTexture maia-standing-sprite-right))
        ;; Dispose Maia walking animation textures
        (dispose-animation-textures maia-walking-animation-right)
        (dispose-animation-textures maia-walking-animation-left)

        (.dispose hallway-bg-texture)))))
