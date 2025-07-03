(ns catcafe.core
  (:require [clojure.set])
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

;; ECS Infrastructure
;; Entity management
(def next-entity-id (atom 0))
(def entities (atom #{}))

(defn create-entity! []
  (let [id (swap! next-entity-id inc)]
    (swap! entities conj id)
    id))

(defn destroy-entity! [entity-id]
  (swap! entities disj entity-id))

;; Component management - World state: {entity-id {component-type component-data}}
(def world (atom {}))

(defn add-component! [entity-id component-type component-data]
  (swap! world assoc-in [entity-id component-type] component-data))

(defn get-component [entity-id component-type]
  (get-in @world [entity-id component-type]))

(defn has-component? [entity-id component-type]
  (contains? (get @world entity-id {}) component-type))

(defn remove-component! [entity-id component-type]
  (swap! world update entity-id dissoc component-type))

;; Entity querying
(defn get-entities-with-component [component-type]
  (->> @world
       (filter (fn [[entity-id components]]
                 (contains? components component-type)))
       (map first)
       set))

(defn get-entities-with-components [component-types]
  (if (empty? component-types)
    #{}
    (let [entity-sets (map get-entities-with-component component-types)]
      (apply clojure.set/intersection entity-sets))))

;; Component record definitions
(defrecord Position [x y])
(defrecord Velocity [dx dy speed])
(defrecord AnimationComp [current-anim animations timer frame-duration])
(defrecord SpriteComp [texture width height facing-right])
(defrecord InputComp [keys active-keys])
(defrecord AIComp [type direction-timer direction-change-interval moving-right])
(defrecord BoundsComp [min-x max-x min-y max-y collision-size])
(defrecord RenderOrderComp [layer])

;; Component constructor functions
(defn position [x y] (->Position x y))
(defn velocity [dx dy speed] (->Velocity dx dy speed))
(defn animation-comp [current-anim animations timer frame-duration] (->AnimationComp current-anim animations timer frame-duration))
(defn sprite-comp [texture width height facing-right] (->SpriteComp texture width height facing-right))
(defn input-comp [keys active-keys] (->InputComp keys active-keys))
(defn ai-comp [type direction-timer direction-change-interval moving-right] (->AIComp type direction-timer direction-change-interval moving-right))
(defn bounds-comp [min-x max-x min-y max-y collision-size] (->BoundsComp min-x max-x min-y max-y collision-size))
(defn render-order-comp [layer] (->RenderOrderComp layer))

;; Entity factory functions
(defn create-player-entity! [x y standing-sprite-right standing-sprite-left walking-animation-right walking-animation-left]
  (let [player-id (create-entity!)]
    (add-component! player-id :position (position x y))
    (add-component! player-id :velocity (velocity 0 0 player-speed))
    (add-component! player-id :animation-comp (animation-comp :standing
                                                              {:walking-right walking-animation-right
                                                               :walking-left walking-animation-left}
                                                              0
                                                              animation-frame-duration))
    (add-component! player-id :sprite-comp (sprite-comp {:standing-right standing-sprite-right
                                                         :standing-left standing-sprite-left}
                                                        player-visual-width
                                                        player-visual-height
                                                        true))
    (add-component! player-id :input-comp (input-comp #{:left :right :up :down} #{}))
    (add-component! player-id :bounds-comp (bounds-comp floor-x-min floor-x-max floor-y-min floor-y-max player-collision-size))
    (add-component! player-id :render-order-comp (render-order-comp 2))
    player-id))

(defn create-npc-entity! [x y standing-sprite-right standing-sprite-left walking-animation-right walking-animation-left]
  (let [npc-id (create-entity!)]
    (add-component! npc-id :position (position x y))
    (add-component! npc-id :velocity (velocity 0 0 npc-speed))
    (add-component! npc-id :animation-comp (animation-comp :standing
                                                           {:walking-right walking-animation-right
                                                            :walking-left walking-animation-left}
                                                           0
                                                           animation-frame-duration))
    (add-component! npc-id :sprite-comp (sprite-comp {:standing-right standing-sprite-right
                                                      :standing-left standing-sprite-left}
                                                     npc-visual-width
                                                     npc-visual-height
                                                     true))
    (add-component! npc-id :ai-comp (ai-comp :wandering 0 npc-direction-change-interval true))
    (add-component! npc-id :bounds-comp (bounds-comp floor-x-min floor-x-max floor-y-min floor-y-max player-collision-size))
    (add-component! npc-id :render-order-comp (render-order-comp 1))
    npc-id))

;; ECS Systems
(defn input-system [delta]
  (doseq [entity-id (get-entities-with-components [:input-comp :velocity])]
    (let [input-comp (get-component entity-id :input-comp)
          moving-left? (.isKeyPressed Gdx/input Input$Keys/LEFT)
          moving-right? (.isKeyPressed Gdx/input Input$Keys/RIGHT)
          moving-up? (.isKeyPressed Gdx/input Input$Keys/UP)
          moving-down? (.isKeyPressed Gdx/input Input$Keys/DOWN)
          is-moving (or moving-left? moving-right? moving-up? moving-down?)
          
          new-dx (cond moving-left? -1 moving-right? 1 :else 0)
          new-dy (cond moving-down? -1 moving-up? 1 :else 0)
          
          velocity-comp (get-component entity-id :velocity)
          new-velocity (->Velocity new-dx new-dy (:speed velocity-comp))
          
          sprite-comp (get-component entity-id :sprite-comp)
          new-facing-right (cond moving-right? true moving-left? false :else (:facing-right sprite-comp))
          new-sprite-comp (->SpriteComp (:texture sprite-comp) (:width sprite-comp) (:height sprite-comp) new-facing-right)]
      
      (add-component! entity-id :velocity new-velocity)
      (add-component! entity-id :sprite-comp new-sprite-comp))))

(defn ai-system [delta]
  (doseq [entity-id (get-entities-with-components [:ai-comp :velocity])]
    (let [ai-comp (get-component entity-id :ai-comp)
          new-direction-timer (+ (:direction-timer ai-comp) delta)
          
          [new-moving-right new-timer] (if (>= new-direction-timer (:direction-change-interval ai-comp))
                                         [(not (:moving-right ai-comp)) 0]
                                         [(:moving-right ai-comp) new-direction-timer])
          
          new-dx (if new-moving-right 1 -1)
          velocity-comp (get-component entity-id :velocity)
          new-velocity (->Velocity new-dx 0 (:speed velocity-comp))
          
          new-ai-comp (->AIComp (:type ai-comp) new-timer (:direction-change-interval ai-comp) new-moving-right)
          
          sprite-comp (get-component entity-id :sprite-comp)
          new-sprite-comp (->SpriteComp (:texture sprite-comp) (:width sprite-comp) (:height sprite-comp) new-moving-right)]
      
      (add-component! entity-id :ai-comp new-ai-comp)
      (add-component! entity-id :velocity new-velocity)
      (add-component! entity-id :sprite-comp new-sprite-comp))))

(defn movement-system [delta]
  (doseq [entity-id (get-entities-with-components [:position :velocity :bounds-comp])]
    (let [pos (get-component entity-id :position)
          vel (get-component entity-id :velocity)
          bounds (get-component entity-id :bounds-comp)
          
          new-x (cond
                  (< (:dx vel) 0) (max (:min-x bounds) (+ (:x pos) (* (:dx vel) (:speed vel) delta)))
                  (> (:dx vel) 0) (min (- (:max-x bounds) (:collision-size bounds)) (+ (:x pos) (* (:dx vel) (:speed vel) delta)))
                  :else (:x pos))
          new-y (cond
                  (< (:dy vel) 0) (max (:min-y bounds) (+ (:y pos) (* (:dy vel) (:speed vel) delta)))
                  (> (:dy vel) 0) (min (- (:max-y bounds) (:collision-size bounds)) (+ (:y pos) (* (:dy vel) (:speed vel) delta)))
                  :else (:y pos))
          
          new-position (->Position new-x new-y)]
      
      (add-component! entity-id :position new-position))))

(defn animation-system [delta]
  (doseq [entity-id (get-entities-with-components [:animation-comp :velocity])]
    (let [anim-comp (get-component entity-id :animation-comp)
          vel-comp (get-component entity-id :velocity)
          is-moving (or (not= 0 (:dx vel-comp)) (not= 0 (:dy vel-comp)))
          
          new-timer (if is-moving (+ (:timer anim-comp) delta) 0)
          new-animation-comp (->AnimationComp (:current-anim anim-comp) (:animations anim-comp) new-timer (:frame-duration anim-comp))]
      
      (add-component! entity-id :animation-comp new-animation-comp))))

(defn render-system [batch]
  (let [entities (get-entities-with-components [:position :sprite-comp :render-order-comp])
        sorted-entities (sort-by #(:layer (get-component % :render-order-comp)) entities)]
    
    (doseq [entity-id sorted-entities]
      (let [pos (get-component entity-id :position)
            sprite-comp (get-component entity-id :sprite-comp)
            anim-comp (get-component entity-id :animation-comp)
            vel-comp (get-component entity-id :velocity)
            is-moving (or (not= 0 (:dx vel-comp)) (not= 0 (:dy vel-comp)))
            
            sprite-key (if (:facing-right sprite-comp) :standing-right :standing-left)
            anim-key (if (:facing-right sprite-comp) :walking-right :walking-left)]
        
        (if is-moving
          (let [animation (get (:animations anim-comp) anim-key)
                current-frame (.getKeyFrame animation (:timer anim-comp) true)]
            (.draw batch current-frame
                   (float (:x pos)) (float (:y pos))
                   (float (:width sprite-comp)) (float (:height sprite-comp))))
          (let [sprite (get (:texture sprite-comp) sprite-key)]
            (.setPosition sprite (float (:x pos)) (float (:y pos)))
            (.draw sprite batch)))))))

(def game-state (atom nil))


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

            hallway-bg-texture (Texture. "images/hallway_bg.png")]
        (.setToOrtho camera false 800 600)
        
        ;; Create ECS entities
        (let [player-id (create-player-entity! 400 100 standing-sprite-right standing-sprite-left walking-animation-right walking-animation-left)
              maia-id (create-npc-entity! 200 100 maia-standing-sprite-right maia-standing-sprite-left maia-walking-animation-right maia-walking-animation-left)]
          
          (reset! game-state
                  {
                         :camera camera
                         :batch batch
                         :hallway-bg-texture hallway-bg-texture
                         :player-id player-id
                         :maia-id maia-id}))))

    (render
      []
      (let [{:keys [camera batch hallway-bg-texture]} @game-state
            delta (.getDeltaTime Gdx/graphics)]

        ;; Clear screen
        (.glClearColor Gdx/gl 0 0 0.2 1)
        (.glClear Gdx/gl GL20/GL_COLOR_BUFFER_BIT)

        ;; Update camera
        (.update camera)
        (.setProjectionMatrix batch (.combined camera))

        ;; Run ECS systems
        (input-system delta)
        (ai-system delta)
        (movement-system delta)
        (animation-system delta)

        ;; Draw
        (.begin batch)
        ;; Draw background first
        (.draw batch hallway-bg-texture
               (float 0) (float 0)
               (float 800) (float 600))

        ;; Render all entities
        (render-system batch)

        (.end batch)))

    (dispose
      []
      (let [{:keys [batch standing-sprite-right
                    walking-animation-right walking-animation-left
                    maia-standing-sprite-right maia-walking-animation-right maia-walking-animation-left
                    hallway-bg-texture]} @game-state]
        (.dispose batch)
        (.dispose (.getTexture standing-sprite-right))
        ;; No need to dispose standing-sprite-left as it uses the same texture

        ;; Dispose player walking animation textures
        (dispose-animation-textures walking-animation-right)
        (dispose-animation-textures walking-animation-left)

        ;; Dispose Maia textures
        (.dispose (.getTexture maia-standing-sprite-right))
        ;; Dispose Maia walking animation textures
        (dispose-animation-textures maia-walking-animation-right)
        (dispose-animation-textures maia-walking-animation-left)

        (.dispose hallway-bg-texture)))))
