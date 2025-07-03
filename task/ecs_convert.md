# ECS Conversion Research Results

This document contains the research findings for converting the Cat Cafe game from its current monolithic architecture to an Entity-Component-System (ECS) architecture.

## Current Architecture Analysis

### Overview
The Cat Cafe game currently uses a **monolithic state management approach** with all game data stored in a single atom called `game-state`. The architecture is entirely procedural, with all logic contained within the main `render` loop.

### Current Game State Structure
The game state atom contains:

#### Player Data:
- `:player-x` and `:player-y` - Player position coordinates
- `:animation-time` - Current animation frame timing
- `:is-moving` - Boolean flag for movement state
- `:facing-right` - Boolean for character facing direction

#### NPC (Maia) Data:
- `:maia-x` and `:maia-y` - NPC position coordinates
- `:maia-animation-time` - NPC animation frame timing
- `:maia-facing-right` - NPC facing direction
- `:maia-direction-timer` - Timer for AI direction changes
- `:maia-moving-right` - Current movement direction for AI

#### Rendering Resources:
- Camera, SpriteBatch, textures, sprites, and animations

### Current Update Logic Structure
The update logic is entirely contained within the `render` method with these phases:
1. **Input Processing** - Keyboard input polling and state updates
2. **NPC AI Update** - Direction change timer and movement logic
3. **Rendering** - Background, NPC, and player sprite drawing

### Problems with Current Architecture
- **Tight coupling** between all game logic in single render loop
- **Hard-coded entity handling** - separate code paths for player vs NPC
- **Mixed concerns** - input, AI, animation, and rendering all intertwined
- **Flat state structure** makes it difficult to add new entities
- **Repetitive patterns** for player vs NPC data

## Simple ECS Patterns for Clojure

### Entity Representation
```clojure
;; Entities as simple UUIDs or numeric IDs
(defn create-entity []
  (java.util.UUID/randomUUID))

;; Entity registry
(def entities (atom #{}))

(defn create-entity! []
  (let [entity-id (java.util.UUID/randomUUID)]
    (swap! entities conj entity-id)
    entity-id))
```

### Component Representation
```clojure
;; Using Clojure records (type-safe, performant)
(defrecord Position [x y])
(defrecord Velocity [dx dy])
(defrecord Sprite [texture width height])
(defrecord Animation [current-anim animations timer frame-duration])

;; Constructor functions for convenience
(defn position [x y] (->Position x y))
(defn velocity [dx dy] (->Velocity dx dy))
```

### Entity-Component Relationships
```clojure
;; World state: entity-id -> component-type -> component-data
(def world (atom {}))

;; Add component to entity
(defn add-component! [entity-id component-type component-data]
  (swap! world assoc-in [entity-id component-type] component-data))

;; Get component from entity
(defn get-component [entity-id component-type]
  (get-in @world [entity-id component-type]))

;; Query entities with specific components
(defn get-entities-with-components [component-types]
  (->> @world
       (filter (fn [[entity-id components]]
                 (every? #(contains? components %) component-types)))
       (map first)))
```

### System Implementation
```clojure
;; Systems as simple functions
(defn movement-system [delta-time]
  (doseq [entity-id (get-entities-with-components [:position :velocity])]
    (let [pos (get-component entity-id :position)
          vel (get-component entity-id :velocity)
          new-pos (position (+ (:x pos) (* (:dx vel) delta-time))
                           (+ (:y pos) (* (:dy vel) delta-time)))]
      (add-component! entity-id :position new-pos))))
```

## Systems Analysis for Current Codebase

### 1. Input System
**Current Logic (Lines 168-200):**
- **Reads from:** `Gdx/input` (keyboard state)
- **Modifies:** Player position, animation time, movement state, facing direction

**ECS Conversion:**
```clojure
(defn input-system [entities delta]
  ;; Process entities with Input components
  ;; Read keyboard input and update Velocity components
  ;; Update facing direction based on input)
```

**Components needed:**
- Input component (tracks input bindings and state)
- Velocity component (stores intended movement)

### 2. Movement System
**Current Logic (Lines 181-189):**
- **Reads from:** Player velocity intentions, boundary constants
- **Modifies:** Player position coordinates

**ECS Conversion:**
```clojure
(defn movement-system [entities delta]
  ;; Process entities with Position and Velocity components
  ;; Apply velocity to position with boundary checking
  ;; Handle collision constraints)
```

**Components needed:**
- Position component
- Velocity component
- Bounds component (for collision boundaries)

### 3. Animation System
**Current Logic (Lines 191-193, 257-263):**
- **Reads from:** Movement state, animation time, delta time
- **Modifies:** Animation timer

**ECS Conversion:**
```clojure
(defn animation-system [entities delta]
  ;; Process entities with Animation components
  ;; Update animation timers
  ;; Select appropriate animation frames)
```

**Components needed:**
- Animation component (stores animation data and timer)
- Sprite component (current frame/texture)

### 4. NPC AI System (Maia's Behavior)
**Current Logic (Lines 202-223):**
- **Reads from:** Direction timer, movement direction, change interval
- **Modifies:** NPC position, direction timer, movement state, facing direction

**ECS Conversion:**
```clojure
(defn ai-system [entities delta]
  ;; Process entities with AI components
  ;; Update AI timers and decision making
  ;; Modify velocity based on AI behavior patterns)
```

**Components needed:**
- AI component (behavior type, timers, state)
- Position component
- Velocity component
- Direction component

### 5. Rendering System
**Current Logic (Lines 225-264):**
- **Reads from:** All position, animation, and sprite data
- **Modifies:** None (pure rendering)

**ECS Conversion:**
```clojure
(defn render-system [entities batch camera]
  ;; Process entities with Position and Sprite components
  ;; Sort by render order
  ;; Draw sprites at positions)
```

**Components needed:**
- Position component
- Sprite component
- RenderOrder component (for layering)

## Required Components

Based on the analysis, these components would be needed:

```clojure
;; Position component
{:x 400 :y 100}

;; Velocity component  
{:dx 0 :dy 0 :speed 200}

;; Input component
{:keys #{:left :right :up :down} :active-keys #{}}

;; Animation component
{:current-animation :walking-right
 :animations {:walking-right animation-obj :walking-left animation-obj}
 :timer 0
 :frame-duration 0.1}

;; Sprite component
{:texture texture-obj :width 121.5 :height 175.5 :facing-right true}

;; AI component
{:type :wandering
 :direction-timer 0
 :direction-change-interval 3.0
 :moving-right true}

;; Bounds component
{:min-x 0 :max-x 800 :min-y 0 :max-y 180 :collision-size 81}

;; RenderOrder component
{:layer 1} ;; background=0, npcs=1, player=2, ui=3
```

## System Execution Order

The systems should run in this order each frame:
1. **Input System** - Read input and set velocity intentions
2. **AI System** - Update NPC behavior and set their velocity intentions  
3. **Movement System** - Apply velocities to positions with collision checking
4. **Animation System** - Update animation timers and select frames
5. **Render System** - Draw all sprites in order

## Benefits of ECS Conversion

1. **Separation of Concerns** - Each system handles one responsibility
2. **Reusability** - Components can be mixed/matched for different entity types
3. **Testability** - Systems can be tested independently
4. **Scalability** - Easy to add new entities, components, or behaviors
5. **Performance** - Systems can process entities in batches
6. **Maintainability** - Logic is isolated and easier to debug

## Implementation Strategy

### Phase 1: Core ECS Infrastructure
- Add entity management functions
- Add component management functions
- Add world state structure
- Add entity querying functions

### Phase 2: Component Migration
- Extract Position components from current state
- Extract Animation components
- Extract Sprite/rendering components
- Extract Input components
- Extract AI components

### Phase 3: System Migration
- Convert input handling to Input System
- Convert movement logic to Movement System
- Convert animation logic to Animation System
- Convert AI logic to AI System
- Convert rendering to Render System

### Phase 4: Entity Creation
- Create player entity with appropriate components
- Create Maia NPC entity with appropriate components
- Remove old state management code

## Key Design Decisions

1. **Keep it Simple** - Use basic Clojure data structures (atoms, maps)
2. **No New Namespaces** - Implement within existing `core.clj`
3. **Incremental Migration** - Convert systems one at a time
4. **Maintain Functionality** - Ensure game continues to work throughout conversion
5. **Test Each Phase** - Verify each system works before moving to next

This research provides a clear roadmap for converting the Cat Cafe game to ECS architecture while maintaining simplicity and avoiding unnecessary complexity.