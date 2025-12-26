# Cat Cafe Game

A 2D game built with Clojure and libGDX featuring an Entity-Component-System (ECS) architecture. Control a character (Ysabel) in a cat cafe environment and interact with NPCs (Maia).

## Prerequisites

To run this game, you'll need:

- [Clojure](https://clojure.org/guides/getting_started) (version 1.11.1 or later)
- [Java JDK](https://adoptium.net/) (version 8 or later)
- [Clojure CLI tools](https://clojure.org/guides/deps_and_cli)

## Running the Game

1. Clone this repository:
   ```
   git clone https://github.com/yourusername/catcafe.git
   cd catcafe
   ```

2. Run the game using the Clojure CLI:
   ```
   clj -M:run-game
   ```

3. **Or run with embedded REPL (Recommended for Development):**
   ```
   clj -M:run-game-with-repl
   ```
   This starts the game window AND an nREPL server on port 7889 for interactive development!

4. **Or start standalone REPL:**
   ```
   clojure -M:nrepl
   ```
   Starts nREPL server on port 7888 for development without the game window.

   Note for macOS users: The game uses the `-XstartOnFirstThread` JVM option which is required for LWJGL on macOS.

## Game Controls

- Use the arrow keys (UP, DOWN, LEFT, RIGHT) to move the character around
- Movement is constrained to floor boundaries

## Interactive REPL Development

Connect to the running game via REPL for interactive development:

```clojure
;; Require the REPL helper namespace (use a different alias than 'repl')
(require '[catcafe.repl :as game-repl])

;; View all available commands
(game-repl/list-repl-commands)

;; Debug the ECS state
(game-repl/debug-entities)

;; Get entity positions
(game-repl/get-player-position)
(game-repl/get-maia-position)

;; Control the player programmatically
(game-repl/simulate-move! :right 2)  ; Move right for 2 seconds
(game-repl/stop-all-movement!)       ; Stop all movement
(game-repl/get-active-keys)          ; Check active keys
```

## Architecture

### Entity-Component-System (ECS)

The game uses a clean ECS architecture:

**Components:**
- Position - x, y coordinates
- Velocity - movement speed and direction
- AnimationComp - animation state and timing
- SpriteComp - textures and visual properties
- InputComp - player input handling
- AIComp - NPC behavior (wandering AI)
- BoundsComp - movement boundaries
- RenderOrderComp - rendering layer order

**Systems:**
- Input System - Processes keyboard input (hardware + simulated)
- AI System - NPC wandering behavior
- Movement System - Physics and boundary enforcement
- Animation System - Animation state updates
- Render System - Sprite rendering with layer ordering

**State Management:**
- Entity management via atoms (`entities`, `world`)
- Component data stored in world map: `{entity-id {component-type component-data}}`
- Game rendering resources in `game-state` atom

## Project Structure

- `src/catcafe/core.clj` - Main game loop, ECS systems, and entity factories
- `src/catcafe/repl.clj` - REPL helper functions for interactive development
- `src/catcafe/desktop_launcher.clj` - Desktop application entry point
- `test/catcafe/` - Unit tests for game logic
- `assets/images/` - Game graphics and sprites

## Development

This game is built with:
- Clojure 1.11.1 for game logic
- libGDX 1.11.0 as the game framework
- LWJGL3 backend for hardware acceleration and window management
- Entity-Component-System architecture for modularity

### Running Tests

```bash
clj -X:test
```

### Linting

```bash
clj-kondo --lint src/catcafe/core.clj --config '{:output {:format :json}}'
```

## License

[Add your license information here]
