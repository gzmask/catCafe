(ns catcafe.core
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
(def game-state (atom nil))


(defn create-initial-state
  []
  {:player-x 400
   :player-y 100
   :animation-time 0
   :is-moving false
   :facing-right true})


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


(defn check-collision
  [x1 y1 w1 h1 x2 y2 w2 h2]
  (not (or (< (+ x1 w1) x2)
           (> x1 (+ x2 w2))
           (< (+ y1 h1) y2)
           (> y1 (+ y2 h2)))))


(defn create-game
  []
  (proxy [Game] []
    (create
      []
      (let [camera (OrthographicCamera.)
            batch (SpriteBatch.)
            ;; Create standing textures and sprites
            standing-texture (Texture. "images/ysabelWalkingRight1.png")
            standing-sprite-right (doto (Sprite. standing-texture)
                                    (.setSize player-visual-width player-visual-height))
            standing-sprite-left (doto (Sprite. standing-texture)
                                   (.setSize player-visual-width player-visual-height)
                                   (.flip true false))

            ;; Create walking animations
            walking-animation-right (create-walking-animation-right)
            walking-animation-left (create-walking-animation-left)

            hallway-bg-texture (Texture. "images/hallway_bg.png")]
        (.setToOrtho camera false 800 600)
        (reset! game-state
                (assoc (create-initial-state)
                       :camera camera
                       :batch batch
                       :standing-sprite-right standing-sprite-right
                       :standing-sprite-left standing-sprite-left
                       :walking-animation-right walking-animation-right
                       :walking-animation-left walking-animation-left
                       :hallway-bg-texture hallway-bg-texture))))

    (render
      []
      (let [{:keys [camera batch standing-sprite-right standing-sprite-left
                    walking-animation-right walking-animation-left animation-time
                    player-x player-y hallway-bg-texture]} @game-state
            delta (.getDeltaTime Gdx/graphics)]

        ;; Clear screen
        (.glClearColor Gdx/gl 0 0 0.2 1)
        (.glClear Gdx/gl GL20/GL_COLOR_BUFFER_BIT)

        ;; Update camera
        (.update camera)
        (.setProjectionMatrix batch (.combined camera))

        ;; Handle input
        (let [moving-left? (.isKeyPressed Gdx/input Input$Keys/LEFT)
              moving-right? (.isKeyPressed Gdx/input Input$Keys/RIGHT)
              moving-up? (.isKeyPressed Gdx/input Input$Keys/UP)
              moving-down? (.isKeyPressed Gdx/input Input$Keys/DOWN)
              is-moving (or moving-left? moving-right? moving-up? moving-down?)

              ;; Update facing direction when moving left or right
              facing-right (cond
                             moving-right? true
                             moving-left? false
                             :else (:facing-right @game-state))

              ;; Calculate new position with boundary constraints
              new-x (cond
                      moving-left? (max floor-x-min (- player-x (* player-speed delta)))
                      moving-right? (min (- floor-x-max player-collision-size) (+ player-x (* player-speed delta)))
                      :else player-x)
              new-y (cond
                      moving-down? (max floor-y-min (- player-y (* player-speed delta)))
                      moving-up? (min (- floor-y-max player-collision-size) (+ player-y (* player-speed delta)))
                      :else player-y)

              new-animation-time (if is-moving
                                   (+ animation-time delta)
                                   0)]

          (swap! game-state assoc
                 :player-x new-x
                 :player-y new-y
                 :animation-time new-animation-time
                 :is-moving is-moving
                 :facing-right facing-right))

        ;; Draw
        (.begin batch)
        ;; Draw background first
        (.draw batch hallway-bg-texture
               (float 0) (float 0)
               (float 800) (float 600))  ;; Use the window size

        ;; Draw player
        (let [{:keys [is-moving facing-right]} @game-state
              current-animation (if facing-right
                                  walking-animation-right
                                  walking-animation-left)
              current-standing (if facing-right
                                 standing-sprite-right
                                 standing-sprite-left)]

          (if is-moving
            (let [current-frame (.getKeyFrame current-animation animation-time true)]
              (.draw batch current-frame
                     (float player-x) (float player-y)
                     (float player-visual-width) (float player-visual-height)))
            (do
              (.setPosition current-standing (float player-x) (float player-y))
              (.draw current-standing batch))))
        (.end batch)))

    (dispose
      []
      (let [{:keys [batch standing-sprite-right
                    walking-animation-right walking-animation-left
                    hallway-bg-texture]} @game-state]
        (.dispose batch)
        (.dispose (.getTexture standing-sprite-right))
        ;; No need to dispose standing-sprite-left as it uses the same texture

        (doseq [frame (.getKeyFrames walking-animation-right)]
          (.dispose (.getTexture frame)))
        ;; No need to dispose walking-animation-left frames as they use the same textures

        (.dispose hallway-bg-texture)))))
