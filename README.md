# Cat Cafe Game

A simple 2D game built with Clojure and libGDX where you control a character in a cafe environment.

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

   Note for macOS users: The game uses the `-XstartOnFirstThread` JVM option which is required for LWJGL on macOS.

## Game Controls

- Use the arrow keys (UP, DOWN, LEFT, RIGHT) to move the character around
- Avoid colliding with objects like tables

## Project Structure

- `src/` - Contains the Clojure source code
- `assets/images/` - Contains game graphics and sprites

## Development

This game is built with:
- Clojure for game logic
- libGDX as the game framework
- LWJGL3 for hardware acceleration and window management

## License

[Add your license information here]
