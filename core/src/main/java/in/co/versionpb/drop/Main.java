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
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class Main implements ApplicationListener {

    Texture backgroundTexture;
    Sprite bucketSprite;
    Texture dropTexture;
    Sprite dropSprite;

    Sound dropSound;
    Music music;

    SpriteBatch spriteBatch;
    FitViewport viewport;

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
            backgroundTexture = new Texture("background.png");
        } catch (Exception e) { Gdx.app.error("AssetLoad", "Failed to load background.png", e); }

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
    
    private void restartGame() {
        // Reset game state
        actualScore = 0;
        lives = 2;
        gameOver = false;
        
        // Reset bucket position
        if (bucketSprite != null) {
            bucketSprite.setPosition(0, 0);
            bucketBounds.x = 0;
        }
        
        // Reset drop position
        resetDrop();
        
        Gdx.app.log("Game", "Game restarted with 2 lives");
    }

    @Override
    public void resize(int width, int height) {
        if (width <= 0 || height <= 0) return;
        if (viewport != null) {
            viewport.update(width, height, true);
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
        if (bucketSprite != null && bucketSprite.getTexture() != null) bucketSprite.getTexture().dispose();
        if (dropTexture != null) dropTexture.dispose();
        if (dropSound != null) dropSound.dispose();
        if (music != null) music.dispose();
        if (spriteBatch != null) spriteBatch.dispose();
        if (scoreDisplayFont != null) scoreDisplayFont.dispose();
    }

    private void input() {
        if (gameOver) {
            // Handle replay button click when game is over
            if (Gdx.input.justTouched()) {
                restartGame();
            }
            return;
        }
        
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
        if (gameOver) return; // Stop game logic when game is over
        
        if (dropBounds == null || dropSprite == null || viewport == null || bucketBounds == null) return;
        dropBounds.y -= dropSpeed * Gdx.graphics.getDeltaTime();
        dropSprite.setY(dropBounds.y);

        if (dropBounds.overlaps(bucketBounds)) {
            if (dropSound != null) {
                dropSound.play();
            }
            actualScore++;
            resetDrop();
        } else if (dropBounds.y + dropBounds.height < 0) {
            // Drop touched the ground without being caught - lose a life
            lives--;
            if (lives <= 0) {
                gameOver = true; // Game over when lives reach zero or below
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
            
            // Draw Game Over message if game is over
            if (gameOver) {
                float centerX = viewport.getWorldWidth() / 2;
                float centerY = viewport.getWorldHeight() / 2;
                
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
                
                // Draw Replay button below final score
                scoreDisplayFont.setColor(Color.YELLOW);
                scoreDisplayFont.draw(spriteBatch, "Click to Replay",
                    centerX, centerY - 80,
                    0, Align.center, false
                );
            }
        }
        spriteBatch.end();
    }
}