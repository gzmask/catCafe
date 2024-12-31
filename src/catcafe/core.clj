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
      SpriteBatch
      TextureRegion)))


(def ^:const player-speed 200)
(def ^:const animation-frame-duration 0.1)
(def game-state (atom nil))


(defn create-initial-state
  []
  {:player-x 400
   :player-y 300
   :table-x 200
   :table-y 150
   :animation-time 0
   :is-moving false})


(defn create-walking-animation
  []
  (let [walking1 (Texture. "images/kate_walking1.png")
        walking2 (Texture. "images/kate_walking2.png")
        frames (into-array TextureRegion
                           [(TextureRegion. walking1)
                            (TextureRegion. walking2)])]
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
            player-standing-texture (Texture. "images/kate_standing.png")
            player-walking-texture (create-walking-animation)
            table-texture (Texture. "images/table.png")]
        (.setToOrtho camera false 800 600)
        (reset! game-state
                (assoc (create-initial-state)
                       :camera camera
                       :batch batch
                       :player-standing-texture player-standing-texture
                       :player-walking-texture player-walking-texture
                       :table-texture table-texture))))

    (render
      []
      (let [{:keys [camera batch player-standing-texture
                    player-walking-texture animation-time
                    player-x player-y table-texture
                    table-x table-y]} @game-state
            delta (.getDeltaTime Gdx/graphics)]

        ;; Clear screen
        (.glClearColor Gdx/gl 0 0 0.2 1)
        (.glClear Gdx/gl GL20/GL_COLOR_BUFFER_BIT)

        ;; Update camera
        (.update camera)
        (.setProjectionMatrix batch (.combined camera))

        ;; Handle input with collision
        (let [moving-left? (.isKeyPressed Gdx/input Input$Keys/LEFT)
              moving-right? (.isKeyPressed Gdx/input Input$Keys/RIGHT)
              moving-up? (.isKeyPressed Gdx/input Input$Keys/UP)
              moving-down? (.isKeyPressed Gdx/input Input$Keys/DOWN)
              is-moving (or moving-left? moving-right? moving-up? moving-down?)

              ;; Calculate potential new position
              potential-x (cond
                            moving-left? (- player-x (* player-speed delta))
                            moving-right? (+ player-x (* player-speed delta))
                            :else player-x)
              potential-y (cond
                            moving-down? (- player-y (* player-speed delta))
                            moving-up? (+ player-y (* player-speed delta))
                            :else player-y)

              ;; Check for collision at new position
              ;; Using smaller collision boxes for better feel (adjusting the size and offset)
              player-collision-size 80  ; Smaller than visual size
              table-collision-size 100  ; Smaller than visual size
              collision? (check-collision
                           potential-x potential-y
                           player-collision-size player-collision-size
                           table-x table-y
                           table-collision-size table-collision-size)

              ;; Only update position if there's no collision
              new-x (if collision? player-x potential-x)
              new-y (if collision? player-y potential-y)
              new-animation-time (if is-moving
                                   (+ animation-time delta)
                                   0)]

          (swap! game-state assoc
                 :player-x new-x
                 :player-y new-y
                 :animation-time new-animation-time
                 :is-moving is-moving))

        ;; Draw
        (.begin batch)
        ;; Draw table first (so it appears behind the player)
        (.draw batch table-texture
               (float table-x) (float table-y)
               (float 128) (float 128))  ; Adjust size as needed

        ;; Draw player (existing code)
        (let [current-frame (if (:is-moving @game-state)
                              (.getKeyFrame player-walking-texture animation-time true)
                              player-standing-texture)]
          (.draw batch current-frame
                 (float player-x) (float player-y) (float 128) (float 128)))
        (.end batch)))

    (dispose
      []
      (let [{:keys [batch player-standing-texture
                    player-walking-texture table-texture]} @game-state]
        (.dispose batch)
        (doseq [frame (.getKeyFrames player-walking-texture)]
          (.dispose (.getTexture frame)))
        (.dispose player-standing-texture)
        (.dispose table-texture)))))
