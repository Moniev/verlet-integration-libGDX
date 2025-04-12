package com.moniev.verlet.core;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.moniev.verlet.core.MainEngine.Engine;

/**
 * Main class for initializing and rendering the simulation.
 * Implements {@link ApplicationListener} to create, render, and manage the lifecycle of the application.
 */
public class Main implements ApplicationListener {
    
    public PerspectiveCamera camera; // Camera for 3D perspective rendering.
    public CameraInputController cameraController; // Controller to handle camera input and movement.
    public Engine engine; // The game or simulation engine responsible for processing and updating the scene.
    private BitmapFont font; // Font used for rendering text on the screen.
    public ModelBatch modelBatch; // Used for batching and rendering 3D models efficiently.
    private SpriteBatch spriteBatch; // Used for 2D sprite rendering.

    private boolean renderTree, renderParticles; // Flags to control whether the tree and particles should be rendered.
    private boolean showFPS, showThreads, showMemoryUsage, showParticleCount; // Flags to show various performance metrics like FPS, threads, memory usage, and particle count.
    private boolean paused; // Flag to pause the simulation or game.
    private int loop; // Counter for loop iterations (could be used for timing or limiting frame updates).

    /**
     * Initializes the game or simulation engine, camera, font, and input processor.
     */
    @Override
    public void create () {
        if (Gdx.graphics.isGL30Available()) Gdx.graphics.getGL30().glEnable(GL30.GL_ARRAY_BUFFER);
        Gdx.gl.glLineWidth(1);
        engine = new Engine(12000, 16, 4, 60);
        modelBatch = new ModelBatch();
        
        camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(60f, 0, 0);
        camera.lookAt(0f, 0f, 0f); 
        camera.near = 1f;
        camera.far = 5000f;
        camera.update();

        Gdx.input.setInputProcessor(new Controller(new CameraInputController(camera)));

        font = new BitmapFont();
        spriteBatch = new SpriteBatch();
        renderParticles = true;
        renderTree = false;
        showFPS = true;
        showThreads = true;
        showMemoryUsage = true;
        showParticleCount = true;
        paused = false;
    }

    /**
     * Gets the number of threads currently in use.
     * 
     * @return the number of threads.
     */
    private int getThreads() {
        return Thread.getAllStackTraces().size();
    }

    /**
     * Gets the current memory usage of the application.
     * 
     * @return the memory usage in megabytes.
     */
    private long getMemoryUsage() {
        long totalMemory = Runtime.getRuntime().totalMemory();
        long freeMemory = Runtime.getRuntime().freeMemory();
        return (totalMemory - freeMemory) / (1024 * 1024);
    }

    /**
     * Resizes the game window (not implemented in this example).
     * 
     * @param width the new width of the window.
     * @param height the new height of the window.
     */
    @Override
    public void resize (int width, int height) {
    }

    /**
     * Main rendering loop of the application.
     * Updates the engine, renders the models, and draws performance metrics.
     */
    @Override
    public void render () {        
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClear(GL30.GL_COLOR_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT);
        
        if(!paused) {
            engine.addParticles(loop);
            engine.update();
        }        
        
        modelBatch.begin(camera);        

        if(renderTree) engine.renderTree(modelBatch);
        if(renderParticles) engine.renderParticles(modelBatch);
    
        modelBatch.end();
        
        spriteBatch.begin();
        if(showMemoryUsage) font.draw(spriteBatch, "MEMORY USAGE: " + getMemoryUsage() + "mb", 10, Gdx.graphics.getHeight() - 10);
        if(showParticleCount) font.draw(spriteBatch, "PARTICLES: " + engine.tree.countParticles(engine.tree.root), 10, Gdx.graphics.getHeight() - 25);
        if(showThreads) font.draw(spriteBatch, "THREADS: " + getThreads(), 10, Gdx.graphics.getHeight() - 40);
        if(showFPS) font.draw(spriteBatch, "FPS: " + Gdx.graphics.getFramesPerSecond(), 10, Gdx.graphics.getHeight() - 55);
        if(paused) font.draw(spriteBatch, "PAUSED", Gdx.graphics.getWidth() / 2 - font.getBounds("PAUSED").width / 2, Gdx.graphics.getHeight() / 2);
        spriteBatch.end();
        loop++;
    }

    /**
     * Pauses the application (not implemented in this example).
     */
    @Override
    public void pause () {
    }

    /**
     * Resumes the application (not implemented in this example).
     */
    @Override
    public void resume () {
    }

    /**
     * Disposes of resources used by the application, such as the engine and model batch.
     */
    @Override
    public void dispose () {
        engine.tree.executor.shutdown();
        engine.disposeParticles();
        engine.disposeTree();
        modelBatch.dispose();
    }

    /**
	 * Controller class that handles user input events for camera manipulation
	 * and toggling various simulation settings, including pausing and rendering options.
	 */
	private class Controller implements InputProcessor {

		private final CameraInputController cameraController; // Controller that handles input events for camera movement and interaction.

		/**
		 * Constructor that initializes the Controller with a CameraInputController.
		 *
		 * @param cameraController The CameraInputController used to manage camera input.
		 */
		public Controller(CameraInputController cameraController) {
			this.cameraController = cameraController;
		}

		/**
		 * Handles key press events.
		 * 
		 * @param keycode The keycode of the pressed key.
		 * @return Returns false to allow further processing of the key event.
		 */
		@Override
		public boolean keyDown(int keycode) {
			switch (keycode) {
				case Input.Keys.P: 
					paused = !paused; // Toggles the paused state of the simulation.
					break;
				case Input.Keys.T: 
					renderTree = !renderTree; // Toggles the rendering of the tree.
					break;
				case Input.Keys.ESCAPE: 
					Gdx.app.exit(); // Exits the application when ESC is pressed.
					break;
			}
			return false;
		}

		/**
		 * Handles key release events.
		 *
		 * @param keycode The keycode of the released key.
		 * @return Returns the result of the camera controller's keyUp method.
		 */
		@Override
		public boolean keyUp(int keycode) {
			return cameraController.keyUp(keycode);
		}

		/**
		 * Handles key typed events (characters typed by the user).
		 *
		 * @param character The typed character.
		 * @return Returns the result of the camera controller's keyTyped method.
		 */
		@Override
		public boolean keyTyped(char character) {
			return cameraController.keyTyped(character);
		}

		/**
		 * Handles touch down events (finger touch or mouse click).
		 *
		 * @param screenX The x-coordinate of the touch point.
		 * @param screenY The y-coordinate of the touch point.
		 * @param pointer The pointer (finger or mouse).
		 * @param button The button pressed (for mouse).
		 * @return Returns the result of the camera controller's touchDown method.
		 */
		@Override
		public boolean touchDown(int screenX, int screenY, int pointer, int button) {        
			return cameraController.touchDown(screenX, screenY, pointer, button);
		}

		/**
		 * Handles touch up events (finger release or mouse release).
		 *
		 * @param screenX The x-coordinate of the touch point.
		 * @param screenY The y-coordinate of the touch point.
		 * @param pointer The pointer (finger or mouse).
		 * @param button The button released (for mouse).
		 * @return Returns the result of the camera controller's touchUp method.
		 */
		@Override
		public boolean touchUp(int screenX, int screenY, int pointer, int button) {
			return cameraController.touchUp(screenX, screenY, pointer, button);
		}

		/**
		 * Handles touch dragged events (finger or mouse drag).
		 *
		 * @param screenX The x-coordinate of the touch point.
		 * @param screenY The y-coordinate of the touch point.
		 * @param pointer The pointer (finger or mouse).
		 * @return Returns the result of the camera controller's touchDragged method.
		 */
		@Override
		public boolean touchDragged(int screenX, int screenY, int pointer) {
			return cameraController.touchDragged(screenX, screenY, pointer);
		}

		/**
		 * Handles mouse move events.
		 *
		 * @param screenX The x-coordinate of the mouse.
		 * @param screenY The y-coordinate of the mouse.
		 * @return Returns the result of the camera controller's mouseMoved method.
		 */
		@Override
		public boolean mouseMoved(int screenX, int screenY) {
			return cameraController.mouseMoved(screenX, screenY);
		}

		/**
		 * Handles mouse scroll events.
		 *
		 * @param amount The amount of scroll (positive or negative).
		 * @return Returns the result of the camera controller's scrolled method.
		 */
		@Override
		public boolean scrolled(int amount) {
			return cameraController.scrolled(amount);
		}
	}
}
