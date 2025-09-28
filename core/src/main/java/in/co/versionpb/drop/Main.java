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

    Sound dropSound;
    Music music;

    SpriteBatch spriteBatch;
    FitViewport viewport;
    
    // --- UI Skin System ---
    private Skin uiSkin;
    private Stage uiStage;
    private TextButton level1Button;
    private TextButton level2Button;
    

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
    private int dropsCollected = 0; // Current drops collected
    
    // --- Level Selection ---
    private int currentLevel = 1;
    private boolean level1Completed = false;
    private boolean level2Unlocked = false;
    
    // --- Scoring ---
    private BitmapFont scoreDisplayFont;
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
        viewport = new FitViewport(800, 480); // World units - matches 800x600 window (4:3 aspect ratio)
        actualScore = 0;

        // --- LOAD FONT ---
        try {
            // Use TTF font for high quality rendering
            FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("AlexBrush-Regular.ttf"));
            FreeTypeFontParameter parameter = new FreeTypeFontParameter();
            parameter.size = 24; // Bigger font size for score
            parameter.color = Color.WHITE;
            parameter.borderWidth = 0; // No border/shadow
            parameter.borderColor = Color.WHITE;
            parameter.genMipMaps = true; // Enable mipmaps for better quality
            parameter.minFilter = Texture.TextureFilter.Linear; // Smooth filtering
            parameter.magFilter = Texture.TextureFilter.Linear; // Smooth filtering
            scoreDisplayFont = generator.generateFont(parameter);
            generator.dispose();
            Gdx.app.log("FontLoad", "TTF font loaded successfully");

        } catch (Exception e) {
            Gdx.app.error("FontLoad", "Failed to load TTF font, using default", e);
            // Fallback to default font
            scoreDisplayFont = new BitmapFont();
            scoreDisplayFont.setColor(Color.WHITE);
            Gdx.app.log("FontLoad", "Using default BitmapFont");
        }
        
        // Debug: Check if font was created
        if (scoreDisplayFont != null) {
            Gdx.app.log("FontLoad", "Font created successfully");
        } else {
            Gdx.app.error("FontLoad", "Font creation failed!");
        }
        // --- END FONT LOADING ---

        try {
            backgroundTexture = new Texture("background_level2.png");
        } catch (Exception e) { Gdx.app.error("AssetLoad", "Failed to load background_level2.png", e); }
        
        try {
            levelsBackgroundTexture = new Texture("BlankLevels.png");
        } catch (Exception e) { Gdx.app.error("AssetLoad", "Failed to load BlankLevels.png", e); }

        Texture bucketTexture = null;
        try {
            bucketTexture = new Texture("bucket.png");
        } catch (Exception e) { Gdx.app.error("AssetLoad", "Failed to load bucket.png", e); }

        try {
            dropTexture = new Texture("drop.png");
        } catch (Exception e) { Gdx.app.error("AssetLoad", "Failed to load drop.png", e); }

        if (bucketTexture != null) {
            bucketSprite = new Sprite(bucketTexture);
            bucketSprite.setSize(64, 64); // Pixel-perfect sizing for 800x480 viewport
            bucketSprite.setPosition(0, 0);
            bucketBounds = new Rectangle(bucketSprite.getX(), bucketSprite.getY(), bucketSprite.getWidth(), bucketSprite.getHeight());
        } else { Gdx.app.error("SpriteInit", "bucketTexture is null."); }

        if (dropTexture != null) {
            dropSprite = new Sprite(dropTexture);
            dropSprite.setSize(32, 32); // Pixel-perfect sizing for 800x480 viewport
            dropBounds = new Rectangle();
            dropBounds.width = dropSprite.getWidth();
            dropBounds.height = dropSprite.getHeight();
            resetDrop();
        } else { Gdx.app.error("SpriteInit", "dropTexture is null."); }

        try {
            dropSound = Gdx.audio.newSound(Gdx.files.internal("drop.mp3"));
            music = Gdx.audio.newMusic(Gdx.files.internal("music.mp3"));
        } catch (Exception e) { Gdx.app.error("AssetLoad", "Failed to load audio files", e); }

        spriteBatch = new SpriteBatch();
        
        // Initialize UI skin system
        initializeUISkin();
    }
    
    private void initializeUISkin() {
        // Create skin
        uiSkin = new Skin();
        
        // Add font to skin
        if (scoreDisplayFont != null) {
            uiSkin.add("default-font", scoreDisplayFont);
        }
        
        // Create button style with colored backgrounds
        TextButtonStyle buttonStyle = new TextButtonStyle();
        buttonStyle.font = uiSkin.getFont("default-font");
        
        // Create colored drawables for button states
        if (backgroundTexture != null) {
            // Create texture region drawable and tint it for different button states
            TextureRegionDrawable textureDrawable = new TextureRegionDrawable(new TextureRegion(backgroundTexture));
            buttonStyle.up = textureDrawable.tint(Color.BLUE);
            buttonStyle.down = textureDrawable.tint(Color.DARK_GRAY);
            buttonStyle.over = textureDrawable.tint(new Color(0.5f, 0.8f, 1f, 1f));
        }
        
        // Set text colors
        buttonStyle.fontColor = Color.WHITE;
        buttonStyle.downFontColor = Color.BLACK;
        
        // Add style to skin
        uiSkin.add("default", buttonStyle);
        
        // Create UI stage
        uiStage = new Stage(viewport);
        
        // Create Level 1 button
        String level1Text = level1Completed ? "Level 1 ★★★" : "Level 1";
        level1Button = new TextButton(level1Text, uiSkin);
        level1Button.setSize(250, 80);
        level1Button.setPosition(
            viewport.getWorldWidth() / 2 - level1Button.getWidth() / 2,
            viewport.getWorldHeight() / 2 - level1Button.getHeight() / 2 + 50
        );
        
        // Create Level 2 button
        String level2Text = level2Unlocked ? "Level 2" : "Level 2 (Locked)";
        level2Button = new TextButton(level2Text, uiSkin);
        level2Button.setSize(250, 80);
        level2Button.setPosition(
            viewport.getWorldWidth() / 2 - level2Button.getWidth() / 2,
            viewport.getWorldHeight() / 2 - level2Button.getHeight() / 2 - 50
        );
        
        // Disable Level 2 button if locked
        if (!level2Unlocked) {
            level2Button.setDisabled(true);
        }
        
        // Add buttons to stage
        uiStage.addActor(level1Button);
        uiStage.addActor(level2Button);
        
        // Set input processor to UI stage for levels screen
        Gdx.input.setInputProcessor(uiStage);
        
        Gdx.app.log("UISkin", "UI Skin system initialized successfully");
        Gdx.app.log("UISkin", "Level 1 button position: " + level1Button.getX() + ", " + level1Button.getY());
        Gdx.app.log("UISkin", "Level 2 button position: " + level2Button.getX() + ", " + level2Button.getY());
        Gdx.app.log("UISkin", "Button size: " + level1Button.getWidth() + " x " + level1Button.getHeight());
        Gdx.app.log("UISkin", "Viewport size: " + viewport.getWorldWidth() + " x " + viewport.getWorldHeight());
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
                    }
                }
            }
            
            Gdx.app.log("Config", "Level 1 config loaded - Speed: " + configDropSpeed + 
                       ", Drops: " + configDropsToCollect + ", Lives: " + configLives);
        } catch (Exception e) {
            Gdx.app.error("Config", "Failed to load Level 1 config, using defaults", e);
            // Use default values
            configDropSpeed = 200f;
            configDropsToCollect = 10;
            configLives = 5;
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
                    }
                }
            }
            
            Gdx.app.log("Config", "Level 2 config loaded - Speed: " + configDropSpeed + 
                       ", Drops: " + configDropsToCollect + ", Lives: " + configLives);
        } catch (Exception e) {
            Gdx.app.error("Config", "Failed to load Level 2 config, using defaults", e);
            // Use default values
            configDropSpeed = 200f;
            configDropsToCollect = 10;
            configLives = 2;
        }
    }
    
    private void refreshLevelSelectionUI() {
        if (uiStage == null || uiSkin == null) return;
        
        // Clear existing buttons
        uiStage.clear();
        
        // Recreate Level 1 button with updated text
        String level1Text = level1Completed ? "Level 1 ★★★" : "Level 1";
        level1Button = new TextButton(level1Text, uiSkin);
        level1Button.setSize(250, 80);
        level1Button.setPosition(
            viewport.getWorldWidth() / 2 - level1Button.getWidth() / 2,
            viewport.getWorldHeight() / 2 - level1Button.getHeight() / 2 + 50
        );
        
        // Recreate Level 2 button with updated text
        String level2Text = level2Unlocked ? "Level 2" : "Level 2 (Locked)";
        level2Button = new TextButton(level2Text, uiSkin);
        level2Button.setSize(250, 80);
        level2Button.setPosition(
            viewport.getWorldWidth() / 2 - level2Button.getWidth() / 2,
            viewport.getWorldHeight() / 2 - level2Button.getHeight() / 2 - 50
        );
        
        // Disable Level 2 button if locked
        if (!level2Unlocked) {
            level2Button.setDisabled(true);
        }
        
        // Add buttons back to stage
        uiStage.addActor(level1Button);
        uiStage.addActor(level2Button);
        
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
        
        // Reset drop position
        resetDrop();
        
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
        
        if (dropBounds == null || dropSprite == null || viewport == null || bucketBounds == null) return;
        dropBounds.y -= dropSpeed * Gdx.graphics.getDeltaTime();
        dropSprite.setY(dropBounds.y);

        if (dropBounds.overlaps(bucketBounds)) {
            if (dropSound != null) {
                dropSound.play();
            }
            actualScore++;
            dropsCollected++;
            resetDrop();
            
            // Check win condition for both levels
            if (dropsCollected >= configDropsToCollect) {
                currentState = GameState.GAME_OVER;
                
                // Mark level as completed and unlock next level
                if (currentLevel == 1) {
                    level1Completed = true;
                    level2Unlocked = true;
                    Gdx.app.log("Game", "Level 1 completed! Level 2 unlocked!");
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
            if (dropSprite != null) dropSprite.draw(spriteBatch);

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
                
                // Draw the level text in the top-center
                float levelX = viewport.getWorldWidth() / 2; // Center horizontally
                float levelY = viewport.getWorldHeight() - padding; // Top side
                
                scoreDisplayFont.setColor(Color.YELLOW);
                scoreDisplayFont.draw(spriteBatch, "Level " + String.valueOf(currentLevel),
                    levelX, levelY,
                    0, Align.center, false
                );
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
        float buttonWidth = 250f;
        float buttonHeight = 80f;
        
        // Draw Level 1 button (above center)
        float level1X = centerX - buttonWidth / 2;
        float level1Y = centerY - buttonHeight / 2 + 50;
        
        spriteBatch.setColor(0.3f, 0.6f, 0.9f, 1f);
        spriteBatch.draw(backgroundTexture, level1X, level1Y, buttonWidth, buttonHeight);
        
        // Draw Level 2 button (below center)
        float level2X = centerX - buttonWidth / 2;
        float level2Y = centerY - buttonHeight / 2 - 50;
        
        spriteBatch.setColor(0.3f, 0.6f, 0.9f, 1f);
        spriteBatch.draw(backgroundTexture, level2X, level2Y, buttonWidth, buttonHeight);
        
        // Draw button texts
        spriteBatch.setColor(Color.WHITE);
        scoreDisplayFont.setColor(Color.WHITE);
        
        String level1Text = level1Completed ? "Level 1 ★★★" : "Level 1";
        String level2Text = level2Unlocked ? "Level 2" : "Level 2 (Locked)";
        
        scoreDisplayFont.draw(spriteBatch, level1Text, centerX, centerY + 50, 0, Align.center, false);
        
        if (level2Unlocked) {
            scoreDisplayFont.setColor(Color.WHITE);
        } else {
            scoreDisplayFont.setColor(Color.GRAY);
        }
        scoreDisplayFont.draw(spriteBatch, level2Text, centerX, centerY - 50, 0, Align.center, false);
        
        spriteBatch.end();
    }
}