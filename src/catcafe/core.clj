(ns catcafe.core
  (:import [com.badlogic.gdx Game Gdx Graphics Input$Keys]
           [com.badlogic.gdx.graphics GL20 OrthographicCamera]
           [com.badlogic.gdx.graphics.g2d SpriteBatch TextureRegion Animation]
           [com.badlogic.gdx.graphics Texture]))
 
(def ^:const player-speed 200)
(def ^:const animation-frame-duration 0.1)
(def game-state (atom nil))

(defn create-initial-state []
  {:player-x 400 
   :player-y 300
   :animation-time 0
   :is-moving false})

(defn create-walking-animation []
  (let [walking1 (Texture. "images/kate_walking1.png")
        walking2 (Texture. "images/kate_walking2.png")
        frames (into-array TextureRegion 
                         [(TextureRegion. walking1)
                          (TextureRegion. walking2)])]
    (Animation. (float animation-frame-duration) frames)))

(defn create-game []
  (proxy [Game] []
    (create []
      (let [camera (OrthographicCamera.)
            batch (SpriteBatch.)
            player-standing-texture (Texture. "images/kate_standing.png")
	    player-walking-texture (create-walking-animation)]
        (.setToOrtho camera false 800 600)
        (reset! game-state 
                (assoc (create-initial-state)
                       :camera camera
                       :batch batch
                       :player-standing-texture player-standing-texture
                       :player-walking-texture player-walking-texture))))

    (render []
      (let [{:keys [camera batch player-standing-texture
		    player-walking-texture animation-time
                    player-x player-y]} @game-state
            delta (.getDeltaTime Gdx/graphics)]
        
        ; Clear screen
        (.glClearColor Gdx/gl 0 0 0.2 1)
        (.glClear Gdx/gl GL20/GL_COLOR_BUFFER_BIT)

        ; Update camera
        (.update camera)
        (.setProjectionMatrix batch (.combined camera))

        ; Handle input
        (let [moving-left? (.isKeyPressed Gdx/input Input$Keys/LEFT)
              moving-right? (.isKeyPressed Gdx/input Input$Keys/RIGHT)
              moving-up? (.isKeyPressed Gdx/input Input$Keys/UP)
              moving-down? (.isKeyPressed Gdx/input Input$Keys/DOWN)
              is-moving (or moving-left? moving-right? moving-up? moving-down?)
              new-x (cond
                     moving-left? (- player-x (* player-speed delta))
                     moving-right? (+ player-x (* player-speed delta))
                     :else player-x)
              new-y (cond
                     moving-down? (- player-y (* player-speed delta))
                     moving-up? (+ player-y (* player-speed delta))
                     :else player-y)
              new-animation-time (if is-moving
                                 (+ animation-time delta)
                                 0)]
          
          (swap! game-state assoc
                 :player-x new-x
                 :player-y new-y
                 :animation-time new-animation-time
                 :is-moving is-moving))

        ; Draw
	(.begin batch)
        (let [current-frame (if (:is-moving @game-state)
                             (.getKeyFrame player-walking-texture animation-time true)
                             player-standing-texture)]
          (.draw batch current-frame
                 (float player-x) (float player-y) (float 128) (float 128)))
        (.end batch)))

    (dispose []
      (let [{:keys [batch player-standing-texture player-walking-texture]} @game-state]
        (.dispose batch)
	(.dispose player-walking-texture)
        (.dispose player-standing-texture)))))
