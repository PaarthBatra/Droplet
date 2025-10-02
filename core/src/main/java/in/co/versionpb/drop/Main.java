package in.co.versionpb.drop;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class Main implements ApplicationListener {

    Texture backgroundTexture;
    Texture levelsBackgroundTexture;
    Sprite bucketSprite;
    Texture dropTexture;
    Sprite dropSprite;

    // Multiple drops system (dynamic based on configuration)
    private Sprite[] dropSprites;
    private Rectangle[] dropBoundsArray;
    private int maxDrops = 10; // Maximum possible drops (will be resized based on config)
    private float[] dropSpawnTimers; // Individual timers for each drop
    private boolean[] dropActive; // Track which drops are active

    Sound dropSound;
    Music music;

    SpriteBatch spriteBatch;
    FitViewport viewport;
    
    // --- UI Skin System ---
    private Skin uiSkin;
    private Stage uiStage;
    private TextButton level1Button;
    private TextButton level2Button;
    private TextButton level3Button;
    private TextButton level4Button;
    
    // --- Music Control ---
    private TextButton muteButton;
    private boolean isMusicMuted = false;
    

    // --- Game States ---
    private enum GameState {
        LEVELS_SCREEN,
        PLAYING,
        GAME_OVER
    }
    private GameState currentState = GameState.LEVELS_SCREEN;
    
    // --- Configuration Variables ---
    private float configDropSpeed = 200f; // Current drop speed
    private int configDropsToCollect = 10; // Drops needed to win
    private int configLives = 5; // Number of lives
    private int configDropsAtATime = 2; // Number of drops that spawn simultaneously
    private float configSpawnDelay = 0.5f; // Time difference between spawning drops
    private int dropsCollected = 0; // Current drops collected
    
    // --- Level Selection ---
    private int currentLevel = 1;
    private boolean level1Completed = false;
    private boolean level2Unlocked = false;
    private boolean level3Unlocked = false;
    private boolean level4Unlocked = false;
    
    // --- Scoring ---
    private BitmapFont scoreDisplayFont;
    private BitmapFont levelDisplayFont; // Smaller font for level text
    private int actualScore;
    private int lives = 2;
    private boolean gameOver = false;
    // --- End Scoring ---

    float bucketSpeed = 300f; // Faster movement for keyboard controls
    float dropSpeed = 200f; // Much faster drop speed

    private final Vector3 touchPos = new Vector3();
    private Rectangle dropBounds;
    private Rectangle bucketBounds;

    @Override
    public void create() {
        viewport = new FitViewport(800, 480);
        actualScore = 0;

        // Load font - use simple default font
        scoreDisplayFont = new BitmapFont();
        scoreDisplayFont.setColor(Color.WHITE);
        levelDisplayFont = new BitmapFont();
        levelDisplayFont.setColor(Color.YELLOW);

        // Load textures
        try {
            backgroundTexture = new Texture("background_level2.png");
        } catch (Exception e) { 
            Gdx.app.error("AssetLoad", "Failed to load background_level2.png", e); 
        }
        
        try {
            levelsBackgroundTexture = new Texture("BlankLevels.png");
        } catch (Exception e) { 
            Gdx.app.error("AssetLoad", "Failed to load BlankLevels.png", e); 
        }

        Texture bucketTexture = null;
        try {
            bucketTexture = new Texture("bucket.png");
        } catch (Exception e) { 
            Gdx.app.error("AssetLoad", "Failed to load bucket.png", e); 
        }

        try {
            dropTexture = new Texture("drop.png");
        } catch (Exception e) { 
            Gdx.app.error("AssetLoad", "Failed to load drop.png", e); 
        }

        if (bucketTexture != null) {
            bucketSprite = new Sprite(bucketTexture);
            bucketSprite.setSize(64, 64);
            bucketSprite.setPosition(0, 0);
            bucketBounds = new Rectangle(bucketSprite.getX(), bucketSprite.getY(), bucketSprite.getWidth(), bucketSprite.getHeight());
        }

        if (dropTexture != null) {
            dropSprite = new Sprite(dropTexture);
            dropSprite.setSize(32, 32);
            dropBounds = new Rectangle();
            dropBounds.width = dropSprite.getWidth();
            dropBounds.height = dropSprite.getHeight();
            resetDrop();
            
            // Initialize multiple drops system (will be resized based on config)
            dropSprites = new Sprite[maxDrops];
            dropBoundsArray = new Rectangle[maxDrops];
            dropSpawnTimers = new float[maxDrops];
            dropActive = new boolean[maxDrops];
            
            for (int i = 0; i < maxDrops; i++) {
                dropSprites[i] = new Sprite(dropTexture);
                dropSprites[i].setSize(32, 32);
                dropBoundsArray[i] = new Rectangle();
                dropBoundsArray[i].width = dropSprites[i].getWidth();
                dropBoundsArray[i].height = dropSprites[i].getHeight();
                dropActive[i] = false; // Start with no active drops
                dropSpawnTimers[i] = 0f; // Will be set based on config
                resetDropAt(i); // Initialize drops off-screen
            }
        }

        try {
            dropSound = Gdx.audio.newSound(Gdx.files.internal("drop.mp3"));
            music = Gdx.audio.newMusic(Gdx.files.internal("music.mp3"));
        } catch (Exception e) { 
            Gdx.app.error("AssetLoad", "Failed to load audio files", e); 
        }

        spriteBatch = new SpriteBatch();
        
        // Initialize simple UI
        initializeSimpleUI();
    }
    
    private void initializeSimpleUI() {
        // Enhanced UI initialization with attractive styling
        uiSkin = new Skin();
        uiStage = new Stage(viewport);
        
        // Create attractive button style with backgrounds
        TextButtonStyle buttonStyle = new TextButtonStyle();
        buttonStyle.font = scoreDisplayFont;
        
        // Create colored backgrounds for buttons
        if (backgroundTexture != null) {
            // Create texture region drawable for button backgrounds
            TextureRegionDrawable textureDrawable = new TextureRegionDrawable(new TextureRegion(backgroundTexture));
            
            // Different colors for different button states
            buttonStyle.up = textureDrawable.tint(new Color(0.2f, 0.6f, 1f, 0.9f)); // Blue
            buttonStyle.down = textureDrawable.tint(new Color(0.1f, 0.3f, 0.7f, 1f)); // Darker blue
            buttonStyle.over = textureDrawable.tint(new Color(0.4f, 0.8f, 1f, 1f)); // Lighter blue
        }
        
        // Enhanced text styling
        buttonStyle.fontColor = Color.WHITE;
        buttonStyle.downFontColor = Color.YELLOW;
        buttonStyle.overFontColor = new Color(1f, 1f, 0.8f, 1f); // Light yellow
        
        uiSkin.add("default", buttonStyle);
        
        // Create level buttons with enhanced styling
        String level1Text = level1Completed ? "Level 1 ★" : "Level 1";
        level1Button = new TextButton(level1Text, uiSkin);
        level1Button.setSize(180, 70); // Slightly larger for better appearance
        level1Button.setPosition(50, viewport.getWorldHeight() / 2 - 35);
        
        String level2Text = level2Unlocked ? "Level 2" : "Level 2 (Locked)";
        level2Button = new TextButton(level2Text, uiSkin);
        level2Button.setSize(180, 70);
        level2Button.setPosition(viewport.getWorldWidth() / 3 - 90, viewport.getWorldHeight() / 2 - 35);
        
        String level3Text = level3Unlocked ? "Level 3" : "Level 3 (Locked)";
        level3Button = new TextButton(level3Text, uiSkin);
        level3Button.setSize(180, 70);
        level3Button.setPosition(2 * viewport.getWorldWidth() / 3 - 90, viewport.getWorldHeight() / 2 - 35);
        
        String level4Text = level4Unlocked ? "Level 4" : "Level 4 (Locked)";
        level4Button = new TextButton(level4Text, uiSkin);
        level4Button.setSize(180, 70);
        level4Button.setPosition(viewport.getWorldWidth() - 230, viewport.getWorldHeight() / 2 - 35);
        
        // Disable locked buttons
        if (!level2Unlocked) {
            level2Button.setDisabled(true);
            level2Button.getStyle().fontColor = Color.GRAY;
        }
        if (!level3Unlocked) {
            level3Button.setDisabled(true);
            level3Button.getStyle().fontColor = Color.GRAY;
        }
        if (!level4Unlocked) {
            level4Button.setDisabled(true);
            level4Button.getStyle().fontColor = Color.GRAY;
        }
        
        // Create mute button
        muteButton = new TextButton("🔊 Mute", uiSkin);
        muteButton.setSize(120, 50);
        muteButton.setPosition(viewport.getWorldWidth() - 130, viewport.getWorldHeight() - 60);
        
        // Add buttons to stage
        uiStage.addActor(level1Button);
        uiStage.addActor(level2Button);
        uiStage.addActor(level3Button);
        uiStage.addActor(level4Button);
        uiStage.addActor(muteButton);
        
        // Set input processor
        Gdx.input.setInputProcessor(uiStage);
    }
    
    private void loadLevel1Config() {
        try {
            String configContent = Gdx.files.internal("level1_config.txt").readString();
            String[] lines = configContent.split("\n");
            
            for (String line : lines) {
                line = line.trim();
                // Skip comments and empty lines
                if (line.startsWith("#") || line.isEmpty()) continue;
                
                String[] parts = line.split("=");
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String value = parts[1].trim();
                    
                    switch (key) {
                        case "drop_speed":
                            configDropSpeed = Float.parseFloat(value);
                            break;
                        case "drops_to_collect":
                            configDropsToCollect = Integer.parseInt(value);
                            break;
                        case "lives":
                            configLives = Integer.parseInt(value);
                            break;
                        case "drops_at_a_time":
                            configDropsAtATime = Integer.parseInt(value);
                            break;
                        case "spawn_delay":
                            configSpawnDelay = Float.parseFloat(value);
                            break;
                    }
                }
            }
            
            Gdx.app.log("Config", "Level 1 config loaded - Speed: " + configDropSpeed + 
                       ", Drops: " + configDropsToCollect + ", Lives: " + configLives +
                       ", Drops at a time: " + configDropsAtATime + ", Spawn delay: " + configSpawnDelay);
        } catch (Exception e) {
            Gdx.app.error("Config", "Failed to load Level 1 config, using defaults", e);
            // Use default values
            configDropSpeed = 200f;
            configDropsToCollect = 10;
            configLives = 5;
            configDropsAtATime = 2;
            configSpawnDelay = 0.5f;
        }
    }
    
    private void loadLevel2Config() {
        try {
            String configContent = Gdx.files.internal("level2_config.txt").readString();
            String[] lines = configContent.split("\n");
            
            for (String line : lines) {
                line = line.trim();
                // Skip comments and empty lines
                if (line.startsWith("#") || line.isEmpty()) continue;
                
                String[] parts = line.split("=");
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String value = parts[1].trim();
                    
                    switch (key) {
                        case "drop_speed":
                            configDropSpeed = Float.parseFloat(value);
                            break;
                        case "drops_to_collect":
                            configDropsToCollect = Integer.parseInt(value);
                            break;
                        case "lives":
                            configLives = Integer.parseInt(value);
                            break;
                        case "drops_at_a_time":
                            configDropsAtATime = Integer.parseInt(value);
                            break;
                        case "spawn_delay":
                            configSpawnDelay = Float.parseFloat(value);
                            break;
                    }
                }
            }
            
            Gdx.app.log("Config", "Level 2 config loaded - Speed: " + configDropSpeed + 
                       ", Drops: " + configDropsToCollect + ", Lives: " + configLives +
                       ", Drops at a time: " + configDropsAtATime + ", Spawn delay: " + configSpawnDelay);
        } catch (Exception e) {
            Gdx.app.error("Config", "Failed to load Level 2 config, using defaults", e);
            // Use default values
            configDropSpeed = 200f;
            configDropsToCollect = 10;
            configLives = 2;
        }
    }
    
    private void loadLevel3Config() {
        try {
            String configContent = Gdx.files.internal("level3_config.txt").readString();
            String[] lines = configContent.split("\n");
            
            for (String line : lines) {
                line = line.trim();
                // Skip comments and empty lines
                if (line.startsWith("#") || line.isEmpty()) continue;
                
                String[] parts = line.split("=");
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String value = parts[1].trim();
                    
                    switch (key) {
                        case "drop_speed":
                            configDropSpeed = Float.parseFloat(value);
                            break;
                        case "drops_to_collect":
                            configDropsToCollect = Integer.parseInt(value);
                            break;
                        case "lives":
                            configLives = Integer.parseInt(value);
                            break;
                        case "drops_at_a_time":
                            configDropsAtATime = Integer.parseInt(value);
                            break;
                        case "spawn_delay":
                            configSpawnDelay = Float.parseFloat(value);
                            break;
                    }
                }
            }
            
            Gdx.app.log("Config", "Level 3 config loaded - Speed: " + configDropSpeed + 
                       ", Drops: " + configDropsToCollect + ", Lives: " + configLives +
                       ", Drops at a time: " + configDropsAtATime + ", Spawn delay: " + configSpawnDelay);
        } catch (Exception e) {
            Gdx.app.error("Config", "Failed to load Level 3 config, using defaults", e);
            // Use default values
            configDropSpeed = 300f;
            configDropsToCollect = 7;
            configLives = 2;
        }
    }
    
    private void loadLevel4Config() {
        try {
            String configContent = Gdx.files.internal("level4_config.txt").readString();
            String[] lines = configContent.split("\n");
            
            for (String line : lines) {
                line = line.trim();
                // Skip comments and empty lines
                if (line.startsWith("#") || line.isEmpty()) continue;
                
                String[] parts = line.split("=");
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String value = parts[1].trim();
                    
                    switch (key) {
                        case "drop_speed":
                            configDropSpeed = Float.parseFloat(value);
                            break;
                        case "drops_to_collect":
                            configDropsToCollect = Integer.parseInt(value);
                            break;
                        case "lives":
                            configLives = Integer.parseInt(value);
                            break;
                        case "drops_at_a_time":
                            configDropsAtATime = Integer.parseInt(value);
                            break;
                        case "spawn_delay":
                            configSpawnDelay = Float.parseFloat(value);
                            break;
                    }
                }
            }
            
            Gdx.app.log("Config", "Level 4 config loaded - Speed: " + configDropSpeed + 
                       ", Drops: " + configDropsToCollect + ", Lives: " + configLives +
                       ", Drops at a time: " + configDropsAtATime + ", Spawn delay: " + configSpawnDelay);
        } catch (Exception e) {
            Gdx.app.error("Config", "Failed to load Level 4 config, using defaults", e);
            // Use default values
            configDropSpeed = 300f;
            configDropsToCollect = 7;
            configLives = 2;
        }
    }
    
    private void refreshLevelSelectionUI() {
        if (uiStage == null || uiSkin == null) return;
        
        // Clear existing buttons
        uiStage.clear();
        
        // Recreate Level 1 button with updated text (far left)
        String level1Text = level1Completed ? "Level 1 ★★★" : "Level 1";
        level1Button = new TextButton(level1Text, uiSkin);
        level1Button.setSize(150, 60);
        level1Button.setPosition(
            50, // Far left side
            viewport.getWorldHeight() / 2 - level1Button.getHeight() / 2
        );
        
        // Recreate Level 2 button with updated text (left-center)
        String level2Text = level2Unlocked ? "Level 2" : "Level 2 (Locked)";
        level2Button = new TextButton(level2Text, uiSkin);
        level2Button.setSize(150, 60);
        level2Button.setPosition(
            viewport.getWorldWidth() / 3 - level2Button.getWidth() / 2, // Left-center
            viewport.getWorldHeight() / 2 - level2Button.getHeight() / 2
        );
        
        // Disable Level 2 button if locked
        if (!level2Unlocked) {
            level2Button.setDisabled(true);
        }
        
        // Recreate Level 3 button with updated text (right-center)
        String level3Text = level3Unlocked ? "Level 3" : "Level 3 (Locked)";
        level3Button = new TextButton(level3Text, uiSkin);
        level3Button.setSize(150, 60);
        level3Button.setPosition(
            2 * viewport.getWorldWidth() / 3 - level3Button.getWidth() / 2, // Right-center
            viewport.getWorldHeight() / 2 - level3Button.getHeight() / 2
        );
        
        // Disable Level 3 button if locked
        if (!level3Unlocked) {
            level3Button.setDisabled(true);
        }
        
        // Recreate Level 4 button with updated text (far right)
        String level4Text = level4Unlocked ? "Level 4" : "Level 4 (Locked)";
        level4Button = new TextButton(level4Text, uiSkin);
        level4Button.setSize(150, 60);
        level4Button.setPosition(
            viewport.getWorldWidth() - level4Button.getWidth() - 50, // Far right side
            viewport.getWorldHeight() / 2 - level4Button.getHeight() / 2
        );
        
        // Disable Level 4 button if locked
        if (!level4Unlocked) {
            level4Button.setDisabled(true);
        }
        
        // Recreate mute button
        String muteText = isMusicMuted ? "🔇 Unmute" : "🔊 Mute";
        muteButton = new TextButton(muteText, uiSkin);
        muteButton.setSize(120, 50);
        muteButton.setPosition(viewport.getWorldWidth() - 130, viewport.getWorldHeight() - 60);
        
        // Add buttons back to stage
        uiStage.addActor(level1Button);
        uiStage.addActor(level2Button);
        uiStage.addActor(level3Button);
        uiStage.addActor(level4Button);
        uiStage.addActor(muteButton);
        
        Gdx.app.log("UI", "Level selection UI refreshed - Level 1 completed: " + level1Completed + ", Level 2 unlocked: " + level2Unlocked);
    }

    private void resetDrop() {
        if (viewport == null || dropBounds == null || dropSprite == null) {
            Gdx.app.error("ResetDrop", "Critical components null.");
            return;
        }
        dropBounds.y = viewport.getWorldHeight();
        dropBounds.x = MathUtils.random(0, viewport.getWorldWidth() - dropBounds.width);
        dropSprite.setPosition(dropBounds.x, dropBounds.y);
    }
    
    private void resetDropAt(int index) {
        if (viewport == null || dropBoundsArray == null || dropSprites == null || 
            index < 0 || index >= maxDrops) {
            return;
        }
        
        // Position drops off-screen initially
        dropBoundsArray[index].y = viewport.getWorldHeight() + 100; // Off-screen
        dropBoundsArray[index].x = MathUtils.random(0, viewport.getWorldWidth() - dropBoundsArray[index].width);
        dropSprites[index].setPosition(dropBoundsArray[index].x, dropBoundsArray[index].y);
    }
    
    private void spawnDropAt(int index) {
        if (viewport == null || dropBoundsArray == null || dropSprites == null || 
            index < 0 || index >= maxDrops) {
            return;
        }
        
        // Spawn drop at top of screen
        dropBoundsArray[index].y = viewport.getWorldHeight();
        dropBoundsArray[index].x = MathUtils.random(0, viewport.getWorldWidth() - dropBoundsArray[index].width);
        dropSprites[index].setPosition(dropBoundsArray[index].x, dropBoundsArray[index].y);
    }
    
    private void toggleMusic() {
        if (music != null) {
            if (isMusicMuted) {
                music.play();
                muteButton.setText("🔊 Mute");
                isMusicMuted = false;
                Gdx.app.log("Music", "Music unmuted");
            } else {
                music.pause();
                muteButton.setText("🔇 Unmute");
                isMusicMuted = true;
                Gdx.app.log("Music", "Music muted");
            }
        }
    }
    
    
    private boolean isClickOnLevel(float clickX, float clickY, int level) {
        // Define level button positions (centered)
        float centerX = viewport.getWorldWidth() / 2;
        float centerY = viewport.getWorldHeight() / 2;
        
        float buttonWidth = 250f; // Updated to match new button size
        float buttonHeight = 80f; // Updated to match new button size
        
        // Level 1 button is centered on screen
        if (level == 1) {
            return clickX >= centerX - buttonWidth/2 && clickX <= centerX + buttonWidth/2 &&
                   clickY >= centerY - buttonHeight/2 && clickY <= centerY + buttonHeight/2;
        }
        
        return false;
    }
    
    private void startGame() {
        currentState = GameState.PLAYING;
        
        // Apply configuration settings for both levels
        dropSpeed = configDropSpeed;
        lives = configLives;
        dropsCollected = 0;
        Gdx.app.log("Game", "Level " + currentLevel + " started with config - Speed: " + configDropSpeed + 
                   ", Drops to collect: " + configDropsToCollect + ", Lives: " + configLives);
        
        // Start background music (only if not muted)
        if (music != null && !isMusicMuted) {
            music.setLooping(true);
            music.setVolume(0.5f);
            music.play();
        }
        
        // Switch input processor back to game input
        Gdx.input.setInputProcessor(null);
        Gdx.app.log("Game", "Game started - Level " + currentLevel);
    }
    
    private void restartGame() {
        // Reset game state
        actualScore = 0;
        lives = 2;
        currentState = GameState.PLAYING;
        
        // Reset bucket position
        if (bucketSprite != null) {
            bucketSprite.setPosition(0, 0);
            bucketBounds.x = 0;
        }
        
        // Reset drop positions based on configuration
        if (configDropsAtATime > 1 && dropSprites != null) {
            // Reset multiple drops
            for (int i = 0; i < configDropsAtATime; i++) {
                dropActive[i] = false; // Mark all drops as inactive
                dropSpawnTimers[i] = i * configSpawnDelay; // Dynamic delay based on config
                resetDropAt(i);
            }
        } else {
            // Reset single drop
            resetDrop();
        }
        
        // Switch input processor back to game input
        Gdx.input.setInputProcessor(null);
        
        Gdx.app.log("Game", "Game restarted with 2 lives");
    }

    @Override
    public void resize(int width, int height) {
        if (width <= 0 || height <= 0) return;
        if (viewport != null) {
            viewport.update(width, height, true);
        }
        if (uiStage != null) {
            uiStage.getViewport().update(width, height, true);
        }
    }

    @Override
    public void render() {
        input();
        logic();
        draw();
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void dispose() {
        if (backgroundTexture != null) backgroundTexture.dispose();
        if (levelsBackgroundTexture != null) levelsBackgroundTexture.dispose();
        if (bucketSprite != null && bucketSprite.getTexture() != null) bucketSprite.getTexture().dispose();
        if (dropTexture != null) dropTexture.dispose();
        if (dropSound != null) dropSound.dispose();
        if (music != null) music.dispose();
        if (spriteBatch != null) spriteBatch.dispose();
        if (scoreDisplayFont != null) scoreDisplayFont.dispose();
        if (levelDisplayFont != null) levelDisplayFont.dispose();
        if (uiSkin != null) uiSkin.dispose();
        if (uiStage != null) uiStage.dispose();
    }

    private void input() {
        if (currentState == GameState.LEVELS_SCREEN) {
            // Handle UI skin input
            if (uiStage != null) {
                uiStage.act(Gdx.graphics.getDeltaTime());
                
                // Check if Level 1 button was clicked
                if (level1Button != null && level1Button.isPressed()) {
                    currentLevel = 1;
                    loadLevel1Config();
                    startGame();
                }
                // Check if Level 2 button was clicked (only if unlocked)
                else if (level2Button != null && level2Button.isPressed() && level2Unlocked) {
                    currentLevel = 2;
                    loadLevel2Config();
                    startGame();
                }
                // Check if Level 3 button was clicked (only if unlocked)
                else if (level3Button != null && level3Button.isPressed() && level3Unlocked) {
                    currentLevel = 3;
                    loadLevel3Config();
                    startGame();
                }
                // Check if Level 4 button was clicked (only if unlocked)
                else if (level4Button != null && level4Button.isPressed() && level4Unlocked) {
                    currentLevel = 4;
                    loadLevel4Config();
                    startGame();
                }
                
                // Check if mute button was clicked
                if (muteButton != null && muteButton.isPressed()) {
                    toggleMusic();
                }
            }
            return;
        }
        
        if (currentState == GameState.GAME_OVER) {
            // Handle replay/return button click when game is over
            if (Gdx.input.justTouched()) {
                if (dropsCollected >= configDropsToCollect) {
                    // Level completed - return to level selection
                    currentState = GameState.LEVELS_SCREEN;
                    refreshLevelSelectionUI();
                    Gdx.input.setInputProcessor(uiStage);
                } else {
                    // Level failed - restart current level
                    restartGame();
                }
            }
            return;
        }
        
        if (currentState != GameState.PLAYING) return;
        
        if (bucketSprite == null || viewport == null || bucketBounds == null) return;
        float currentBucketX = bucketSprite.getX();
        float deltaTime = Gdx.graphics.getDeltaTime();
        float newBucketX = currentBucketX;
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) newBucketX += bucketSpeed * deltaTime;
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) newBucketX -= bucketSpeed * deltaTime;
        if (Gdx.input.isTouched()) {
            touchPos.set(Gdx.input.getX(), Gdx.input.getY(), 0);
            viewport.unproject(touchPos);
            newBucketX = touchPos.x - bucketSprite.getWidth() / 2;
        }
        float minBucketX = 0;
        float maxBucketX = viewport.getWorldWidth() - bucketSprite.getWidth();
        newBucketX = MathUtils.clamp(newBucketX, minBucketX, maxBucketX);
        bucketSprite.setX(newBucketX);
        bucketBounds.x = newBucketX;
    }

    private void logic() {
        if (currentState != GameState.PLAYING) return; // Only run game logic when playing
        
        if (viewport == null || bucketBounds == null) return;
        
        // Handle multiple drops if configured
        if (configDropsAtATime > 1 && dropSprites != null && dropBoundsArray != null) {
            handleMultipleDrops();
        } else {
            // Handle single drop
            handleSingleDrop();
        }
    }
    
    private void handleMultipleDrops() {
        float deltaTime = Gdx.graphics.getDeltaTime();
        
        // Check if we need to start a new spawn cycle
        boolean allDropsInactive = true;
        for (int i = 0; i < maxDrops; i++) {
            if (dropActive[i]) {
                allDropsInactive = false;
                break;
            }
        }
        
        // If all drops are inactive, start a new spawn cycle
        if (allDropsInactive) {
            // Reset timers for new cycle based on configuration
            for (int i = 0; i < configDropsAtATime; i++) {
                dropSpawnTimers[i] = i * configSpawnDelay; // Dynamic delay based on config
            }
        }
        
        // Update spawn timers and spawn drops with configured delay
        for (int i = 0; i < configDropsAtATime; i++) {
            if (!dropActive[i]) {
                dropSpawnTimers[i] -= deltaTime; // Countdown timer
                
                // Spawn drop when timer reaches zero
                if (dropSpawnTimers[i] <= 0f) {
                    spawnDropAt(i);
                    dropActive[i] = true;
                }
            }
        }
        
        // Update all active drops
        for (int i = 0; i < configDropsAtATime; i++) {
            if (dropActive[i] && dropBoundsArray[i] != null && dropSprites[i] != null) {
                // Move drop down
                dropBoundsArray[i].y -= dropSpeed * deltaTime;
                dropSprites[i].setY(dropBoundsArray[i].y);
                
                // Check collision with bucket
                if (dropBoundsArray[i].overlaps(bucketBounds)) {
                    if (dropSound != null) {
                        dropSound.play();
                    }
                    
                    // Mute music when drop collides with bucket
                    if (music != null && !isMusicMuted) {
                        music.pause();
                        isMusicMuted = true;
                        if (muteButton != null) {
                            muteButton.setText("🔇 Unmute");
                        }
                        Gdx.app.log("Music", "Music muted due to drop collision");
                    }
                    
                    actualScore++;
                    dropsCollected++;
                    dropActive[i] = false; // Mark drop as inactive
                    resetDropAt(i); // Reset this drop
                    
                    // Check win condition
                    if (dropsCollected >= configDropsToCollect) {
                        currentState = GameState.GAME_OVER;
                        
                        // Mark level as completed and unlock next level
                        if (currentLevel == 1) {
                            level1Completed = true;
                            level2Unlocked = true;
                            Gdx.app.log("Game", "Level 1 completed! Level 2 unlocked!");
                        } else if (currentLevel == 2) {
                            level3Unlocked = true;
                            Gdx.app.log("Game", "Level 2 completed! Level 3 unlocked!");
                        } else if (currentLevel == 3) {
                            level4Unlocked = true;
                            Gdx.app.log("Game", "Level 3 completed! Level 4 unlocked!");
                        }
                        
                        Gdx.app.log("Game", "Level " + currentLevel + " completed! Collected " + dropsCollected + " drops");
                        return;
                    }
                } else if (dropBoundsArray[i].y + dropBoundsArray[i].height < 0) {
                    // Drop touched the ground without being caught - lose a life
                    lives--;
                    dropActive[i] = false; // Mark drop as inactive
                    if (lives <= 0) {
                        currentState = GameState.GAME_OVER; // Game over when lives reach zero or below
                    } else {
                        resetDropAt(i); // Reset this drop
                    }
                }
            }
        }
    }
    
    private void handleSingleDrop() {
        if (dropBounds == null || dropSprite == null) return;
        
        dropBounds.y -= dropSpeed * Gdx.graphics.getDeltaTime();
        dropSprite.setY(dropBounds.y);

        if (dropBounds.overlaps(bucketBounds)) {
            if (dropSound != null) {
                dropSound.play();
            }
            
            // Mute music when drop collides with bucket
            if (music != null && !isMusicMuted) {
                music.pause();
                isMusicMuted = true;
                if (muteButton != null) {
                    muteButton.setText("🔇 Unmute");
                }
                Gdx.app.log("Music", "Music muted due to drop collision");
            }
            
            actualScore++;
            dropsCollected++;
            resetDrop();
            
            // Check win condition for other levels
            if (dropsCollected >= configDropsToCollect) {
                currentState = GameState.GAME_OVER;
                
                // Mark level as completed and unlock next level
                if (currentLevel == 2) {
                    level3Unlocked = true;
                    Gdx.app.log("Game", "Level 2 completed! Level 3 unlocked!");
                } else if (currentLevel == 3) {
                    level4Unlocked = true;
                    Gdx.app.log("Game", "Level 3 completed! Level 4 unlocked!");
                }
                
                Gdx.app.log("Game", "Level " + currentLevel + " completed! Collected " + dropsCollected + " drops");
            }
        } else if (dropBounds.y + dropBounds.height < 0) {
            // Drop touched the ground without being caught - lose a life
            lives--;
            if (lives <= 0) {
                currentState = GameState.GAME_OVER; // Game over when lives reach zero or below
            } else {
                resetDrop(); // Only reset drop if game is still active
            }
        }
    }

    private void draw() {
        ScreenUtils.clear(Color.BLACK);
        if (viewport == null || spriteBatch == null) return;

        viewport.apply();
        spriteBatch.setProjectionMatrix(viewport.getCamera().combined);

        spriteBatch.begin();

        if (currentState == GameState.LEVELS_SCREEN) {
            // Draw levels screen
            if (levelsBackgroundTexture != null) {
                spriteBatch.draw(levelsBackgroundTexture, 0, 0, viewport.getWorldWidth(), viewport.getWorldHeight());
            }
        } else if (currentState == GameState.PLAYING) {
            // Draw game screen
            if (backgroundTexture != null) spriteBatch.draw(backgroundTexture, 0, 0, viewport.getWorldWidth(), viewport.getWorldHeight());
            if (bucketSprite != null) bucketSprite.draw(spriteBatch);
            
            // Draw drops based on configuration
            if (configDropsAtATime > 1 && dropSprites != null) {
                // Draw multiple drops (only active ones)
                for (int i = 0; i < configDropsAtATime; i++) {
                    if (dropActive[i] && dropSprites[i] != null) {
                        dropSprites[i].draw(spriteBatch);
                    }
                }
            } else {
                // Draw single drop
                if (dropSprite != null) dropSprite.draw(spriteBatch);
            }

            if (scoreDisplayFont != null) {
                float padding = 20f; // Pixel units for 800x480 viewport
                
                // Draw the score text in the top-right corner
                float scoreX = viewport.getWorldWidth() - padding; // Right side
                float scoreY = viewport.getWorldHeight() - padding; // Top side
                
                scoreDisplayFont.setColor(Color.WHITE);
                scoreDisplayFont.draw(spriteBatch, String.valueOf(actualScore),
                    scoreX, scoreY,
                    0, Align.topRight, false
                );
                
                // Draw the lives text in the top-left corner
                float livesX = padding; // Left side
                float livesY = viewport.getWorldHeight() - padding; // Top side
                
                scoreDisplayFont.draw(spriteBatch, "Lives: " + String.valueOf(lives),
                    livesX, livesY,
                    0, Align.topLeft, false
                );
                
                // Draw the level text in the top-center with smaller font
                float levelX = viewport.getWorldWidth() / 2; // Center horizontally
                float levelY = viewport.getWorldHeight() - 10; // Very top of screen
                
                if (levelDisplayFont != null) {
                    levelDisplayFont.setColor(Color.YELLOW);
                    levelDisplayFont.draw(spriteBatch, "Level " + String.valueOf(currentLevel),
                        levelX, levelY,
                        0, Align.center, false
                    );
                }
            }
        } else if (currentState == GameState.GAME_OVER) {
            // Draw game over screen
            if (backgroundTexture != null) spriteBatch.draw(backgroundTexture, 0, 0, viewport.getWorldWidth(), viewport.getWorldHeight());
            
            if (scoreDisplayFont != null) {
                float centerX = viewport.getWorldWidth() / 2;
                float centerY = viewport.getWorldHeight() / 2;
                
                // Show different messages based on win/lose condition
                if (dropsCollected >= configDropsToCollect) {
                    scoreDisplayFont.setColor(Color.GREEN);
                    scoreDisplayFont.draw(spriteBatch, "LEVEL " + currentLevel + " COMPLETE!",
                        centerX, centerY,
                        0, Align.center, false
                    );
                    
                    scoreDisplayFont.setColor(Color.WHITE);
                    scoreDisplayFont.draw(spriteBatch, "Drops Collected: " + dropsCollected + "/" + configDropsToCollect,
                        centerX, centerY - 40,
                        0, Align.center, false
                    );
                } else {
                    scoreDisplayFont.setColor(Color.RED);
                    scoreDisplayFont.draw(spriteBatch, "GAME OVER",
                        centerX, centerY,
                        0, Align.center, false
                    );
                    
                    // Draw final score below Game Over
                    scoreDisplayFont.setColor(Color.WHITE);
                    scoreDisplayFont.draw(spriteBatch, "Final Score: " + String.valueOf(actualScore),
                        centerX, centerY - 40,
                        0, Align.center, false
                    );
                }
                
                // Draw appropriate button text
                scoreDisplayFont.setColor(Color.YELLOW);
                if (dropsCollected >= configDropsToCollect) {
                    scoreDisplayFont.draw(spriteBatch, "Click to Return to Levels",
                        centerX, centerY - 80,
                        0, Align.center, false
                    );
                } else {
                    scoreDisplayFont.draw(spriteBatch, "Click to Replay",
                        centerX, centerY - 80,
                        0, Align.center, false
                    );
                }
            }
        }
        
        spriteBatch.end();
        
        // Draw UI elements after sprite batch
        if (currentState == GameState.LEVELS_SCREEN) {
            if (uiStage != null) {
                uiStage.draw();
            } else {
                // Fallback: draw button manually if skin system fails
                drawFallbackButton();
            }
        }
    }
    
    private void drawFallbackButton() {
        if (spriteBatch == null || viewport == null || scoreDisplayFont == null) return;
        
        spriteBatch.begin();
        
        float centerX = viewport.getWorldWidth() / 2;
        float centerY = viewport.getWorldHeight() / 2;
        float buttonWidth = 180f; // Updated to match new button size
        float buttonHeight = 70f; // Updated to match new button size
        
        // Draw Level 1 button (far left) with enhanced styling
        float level1X = 50;
        float level1Y = centerY - buttonHeight / 2;
        
        spriteBatch.setColor(0.2f, 0.6f, 1f, 0.9f); // Blue color
        spriteBatch.draw(backgroundTexture, level1X, level1Y, buttonWidth, buttonHeight);
        
        // Draw Level 2 button (left-center) with enhanced styling
        float level2X = viewport.getWorldWidth() / 3 - buttonWidth / 2;
        float level2Y = centerY - buttonHeight / 2;
        
        if (level2Unlocked) {
            spriteBatch.setColor(0.2f, 0.6f, 1f, 0.9f); // Blue color
        } else {
            spriteBatch.setColor(0.3f, 0.3f, 0.3f, 0.7f); // Gray for locked
        }
        spriteBatch.draw(backgroundTexture, level2X, level2Y, buttonWidth, buttonHeight);
        
        // Draw Level 3 button (right-center) with enhanced styling
        float level3X = 2 * viewport.getWorldWidth() / 3 - buttonWidth / 2;
        float level3Y = centerY - buttonHeight / 2;
        
        if (level3Unlocked) {
            spriteBatch.setColor(0.2f, 0.6f, 1f, 0.9f); // Blue color
        } else {
            spriteBatch.setColor(0.3f, 0.3f, 0.3f, 0.7f); // Gray for locked
        }
        spriteBatch.draw(backgroundTexture, level3X, level3Y, buttonWidth, buttonHeight);
        
        // Draw Level 4 button (far right) with enhanced styling
        float level4X = viewport.getWorldWidth() - buttonWidth - 50;
        float level4Y = centerY - buttonHeight / 2;
        
        if (level4Unlocked) {
            spriteBatch.setColor(0.2f, 0.6f, 1f, 0.9f); // Blue color
        } else {
            spriteBatch.setColor(0.3f, 0.3f, 0.3f, 0.7f); // Gray for locked
        }
        spriteBatch.draw(backgroundTexture, level4X, level4Y, buttonWidth, buttonHeight);
        
        // Draw button texts with enhanced styling
        spriteBatch.setColor(Color.WHITE);
        scoreDisplayFont.setColor(Color.WHITE);
        
        String level1Text = level1Completed ? "Level 1 ★" : "Level 1";
        String level2Text = level2Unlocked ? "Level 2" : "Level 2 (Locked)";
        String level3Text = level3Unlocked ? "Level 3" : "Level 3 (Locked)";
        String level4Text = level4Unlocked ? "Level 4" : "Level 4 (Locked)";
        
        scoreDisplayFont.draw(spriteBatch, level1Text, level1X + buttonWidth / 2, level1Y + buttonHeight / 2, 0, Align.center, false);
        
        if (level2Unlocked) {
            scoreDisplayFont.setColor(Color.WHITE);
        } else {
            scoreDisplayFont.setColor(Color.GRAY);
        }
        scoreDisplayFont.draw(spriteBatch, level2Text, level2X + buttonWidth / 2, level2Y + buttonHeight / 2, 0, Align.center, false);
        
        if (level3Unlocked) {
            scoreDisplayFont.setColor(Color.WHITE);
        } else {
            scoreDisplayFont.setColor(Color.GRAY);
        }
        scoreDisplayFont.draw(spriteBatch, level3Text, level3X + buttonWidth / 2, level3Y + buttonHeight / 2, 0, Align.center, false);
        
        if (level4Unlocked) {
            scoreDisplayFont.setColor(Color.WHITE);
        } else {
            scoreDisplayFont.setColor(Color.GRAY);
        }
        scoreDisplayFont.draw(spriteBatch, level4Text, level4X + buttonWidth / 2, level4Y + buttonHeight / 2, 0, Align.center, false);
        
        spriteBatch.end();
    }
}