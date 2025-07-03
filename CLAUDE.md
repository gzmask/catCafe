# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a 2D game built with Clojure and libGDX. The game simulates a cat cafe environment where players control a character (Ysabel) and interact with NPCs (Maia).

## Rules

for any task: 

 1. create a task folder
 2. create a [task]-todo.md file with the plan that you think carefully to make 
 3. perform each todo and check off them 
 4. add short summary when you check them off
 5. after all todos are checked off, perform test
 5. once task is done, create an overall summary
 6. run the game and ask to verify

## Common Development Commands

### Running the Game
```bash
clj -M:run-game
```
Note: Uses `-XstartOnFirstThread` JVM option required for LWJGL on macOS.

### Running Tests
```bash
clj -X:test
```
This runs all tests via the test runner in `test/catcafe/test_runner.clj`.

### Project Dependencies
Dependencies are managed via `deps.edn`. The project uses:
- Clojure 1.11.1
- libGDX 1.11.0 (game framework)
- LWJGL3 backend for desktop

## Architecture Overview

### Game Loop Structure
The main game logic in `catcafe.core` follows libGDX patterns:
- `create`: Initialize game state, textures, and sprites
- `render`: Handle input, update positions, and draw sprites each frame
- `dispose`: Clean up resources (textures, animations, sprites)

### State Management
Game state is managed through a single atom `game-state` containing:
- Player position and animation state
- NPC (Maia) position and movement AI
- Loaded textures, sprites, and animations
- Camera and rendering resources

### Key Constants and Configuration
Game constants are defined in `catcafe.core`:
- Player/NPC speeds, visual dimensions, collision sizes
- Floor boundaries for movement constraints
- Animation frame durations and timing intervals

### Asset Management
- Textures loaded from `assets/images/` directory
- Walking animations created from texture pairs
- Sprite flipping used for directional animations
- Resource disposal pattern implemented for memory management

### Testing Structure
Tests are organized in `test/catcafe/` with:
- `core_test.clj`: Tests for collision detection and core game functions
- `test_runner.clj`: Centralized test execution

## Code Organization

- `src/catcafe/core.clj`: Main game class, rendering loop, and state management
- `src/catcafe/desktop_launcher.clj`: Desktop application entry point
- `test/catcafe/core_test.clj`: Unit tests for game logic
- `test/catcafe/test_runner.clj`: Test execution utilities

## Development Notes

Animation textures require proper disposal to prevent memory leaks. Use the `dispose-animation-textures` utility function when cleaning up resources.

The game uses a simple collision detection system with rectangular boundaries for movement constraints and entity interactions.