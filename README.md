# Droplet Game 🎮💧

A dynamic and engaging droplet-catching game built with [libGDX](https://libgdx.com/). Catch falling droplets with your bucket while managing multiple drops, music, and progressive difficulty across multiple levels.

## 🎯 Game Overview

Droplet is a fast-paced arcade-style game where players control a bucket to catch falling droplets. The game features multiple levels with increasing difficulty, configurable drop spawning patterns, and engaging gameplay mechanics.

## ✨ Features

### 🎮 Core Gameplay
- **Bucket Control**: Move your bucket left and right to catch falling droplets
- **Multiple Drop Systems**: Each level can spawn 1-4 drops simultaneously
- **Progressive Difficulty**: 4 levels with increasing challenge
- **Lives System**: Lose lives when droplets hit the ground
- **Score Tracking**: Collect droplets to increase your score

### 🎵 Audio Features
- **Background Music**: Immersive background music during gameplay
- **Sound Effects**: Audio feedback when catching droplets
- **Music Control**: Mute/unmute button with visual feedback
- **Auto-Mute**: Music automatically mutes when catching droplets

### 🎨 Visual Design
- **Attractive UI**: Enhanced button styling with color-coded states
- **Level Indicators**: Visual completion markers (★) for finished levels
- **Locked Level Display**: Clear indication of locked vs unlocked levels
- **Responsive Layout**: Adapts to different screen sizes

### ⚙️ Configuration System
- **Per-Level Settings**: Each level has its own configuration file
- **Customizable Parameters**:
  - Drop falling speed
  - Number of drops to collect
  - Number of lives
  - Drops spawning simultaneously
  - Time delay between drop spawns

## 🎮 How to Play

### Controls
- **Mouse/Touch**: Click and drag to move the bucket
- **Keyboard**: Use left/right arrow keys or A/D keys to move
- **UI Buttons**: Click level buttons to start levels, mute button to control music

### Objective
- Catch the required number of droplets to complete each level
- Avoid letting droplets hit the ground (loses a life)
- Complete levels to unlock the next one

### Level Progression
1. **Level 1**: 2 drops, 0.8s delay - Moderate challenge
2. **Level 2**: 2 drops, 0.7s delay - Slightly faster
3. **Level 3**: 3 drops, 0.6s delay - Increased difficulty
4. **Level 4**: 3 drops, 0.5s delay - Expert challenge

## 🛠️ Technical Details

### Architecture
- **Framework**: libGDX (Java-based game development framework)
- **Platforms**: Desktop (LWJGL3), Android
- **Build System**: Gradle
- **UI System**: Scene2D with custom skin

### Key Components
- **Main Game Class**: `Main.java` - Core game logic and state management
- **Configuration Files**: `levelX_config.txt` - Per-level settings
- **Asset Management**: Textures, sounds, music, fonts
- **Input Handling**: Mouse, keyboard, and touch input
- **Collision Detection**: Rectangle-based collision system

### Configuration Parameters
Each level configuration file (`levelX_config.txt`) supports:
```
# Drop falling speed (pixels per second)
drop_speed=200

# Number of drops to collect to win
drops_to_collect=3

# Number of lives player starts with
lives=5

# Number of drops that spawn at the same time
drops_at_a_time=2

# Time difference between spawning drops (in seconds)
spawn_delay=0.5
```

## 🚀 Getting Started

### Prerequisites
- Java 17 or higher
- Gradle (included via wrapper)

### Running the Game

#### Desktop (LWJGL3)
```bash
# Build the project
./gradlew build

# Run the game
./gradlew :lwjgl3:run
```

#### Android
```bash
# Build Android APK
./gradlew :android:assembleDebug

# Install and run on connected device
./gradlew :android:installDebug
```

### Development Commands
```bash
# Clean build
./gradlew clean

# Run tests
./gradlew test

# Generate IDE project files
./gradlew eclipse
./gradlew idea
```

## 📁 Project Structure

```
Droplet/
├── core/                    # Main game logic
│   └── src/main/java/
│       └── in/co/versionpb/drop/
│           └── Main.java     # Core game class
├── lwjgl3/                  # Desktop platform
├── android/                 # Android platform
├── assets/                  # Game assets
│   ├── *.png               # Textures and sprites
│   ├── *.mp3               # Audio files
│   ├── *.ttf               # Fonts
│   └── level*_config.txt   # Level configurations
├── build.gradle            # Project build configuration
└── README.md               # This file
```

## 🎨 Assets

### Graphics
- `background.png` - Main game background
- `background_level2.png` - Level selection background
- `bucket.png` - Player bucket sprite
- `drop.png` - Droplet sprite
- `UI_Skin_data/` - UI skin assets

### Audio
- `music.mp3` - Background music
- `drop.mp3` - Drop collection sound effect

### Fonts
- `AlexBrush-Regular.ttf` - Custom font for UI
- `chillar_100.fnt/png` - Bitmap font assets

## ⚙️ Configuration Guide

### Customizing Levels

To modify level difficulty, edit the corresponding `levelX_config.txt` file:

#### Easy Mode Example
```
drop_speed=150
drops_to_collect=5
lives=10
drops_at_a_time=1
spawn_delay=2.0
```

#### Hard Mode Example
```
drop_speed=400
drops_to_collect=10
lives=2
drops_at_a_time=4
spawn_delay=0.1
```

### Adding New Levels

1. Create a new `levelX_config.txt` file in the `assets/` directory
2. Add the level button logic in `Main.java`
3. Update the level selection UI
4. Add level completion logic

## 🐛 Troubleshooting

### Common Issues

#### Game Won't Start
- Ensure Java 17+ is installed
- Check that all assets are in the `assets/` directory
- Verify Gradle wrapper permissions

#### Configuration Not Loading
- Check file format (no BOM, proper line endings)
- Ensure all required parameters are present
- Verify file is in `assets/` directory

#### Audio Issues
- Check audio file formats (MP3 supported)
- Verify file paths in code
- Ensure system audio is working

### Debug Mode
The game includes extensive logging. Check console output for:
- Configuration loading status
- Game state changes
- Error messages
- Performance metrics

## 🤝 Contributing

### Development Setup
1. Clone the repository
2. Import into your IDE (IntelliJ IDEA recommended)
3. Run `./gradlew build` to verify setup
4. Start developing!

### Code Style
- Follow Java naming conventions
- Use meaningful variable names
- Add comments for complex logic
- Maintain consistent indentation

## 📝 License

This project is generated with [gdx-liftoff](https://github.com/libgdx/gdx-liftoff) and uses the libGDX framework.

## 🙏 Acknowledgments

- [libGDX](https://libgdx.com/) - Game development framework
- [gdx-liftoff](https://github.com/libgdx/gdx-liftoff) - Project template generator
- Game assets and inspiration from the libGDX community

---

**Enjoy playing Droplet!** 🎮💧

For questions or issues, please check the troubleshooting section or create an issue in the project repository.