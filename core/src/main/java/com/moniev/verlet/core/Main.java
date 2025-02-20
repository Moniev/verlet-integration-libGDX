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


public class Main implements ApplicationListener {
	public PerspectiveCamera camera;
	public CameraInputController cameraController;
	public Engine engine;
	private BitmapFont font;
	public ModelBatch modelBatch;
    private SpriteBatch spriteBatch;

	private boolean renderTree, renderParticles;
	private boolean showFPS, showThreads, showMemoryUsage, showParticleCount;
	private boolean paused;
	private int loop;

	@Override
	public void create () {
		if (Gdx.graphics.isGL30Available()) Gdx.graphics.getGL30().glEnable(GL30.GL_ARRAY_BUFFER);
		Gdx.gl.glLineWidth(1);
		engine = new Engine(4000, 16, 4, 60);
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

	private int getThreads() {
		return Thread.getAllStackTraces().size();
	}

	private long getMemoryUsage() {
		long totalMemory = Runtime.getRuntime().totalMemory();
        long freeMemory = Runtime.getRuntime().freeMemory();
        return (totalMemory - freeMemory) / (1024 * 1024);
	}

	@Override
	public void resize (int width, int height) {
	}

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

	@Override
	public void pause () {
	}

	@Override
	public void resume () {
	}

	@Override
	public void dispose () {
		engine.tree.executor.shutdown();
		engine.disposeParticles();
		engine.disposeTree();
		modelBatch.dispose();
	}

	private class Controller implements InputProcessor{
        private final CameraInputController cameraController;

        public Controller(CameraInputController cameraController) {
            this.cameraController = cameraController;
        }

		@Override
        public boolean keyDown(int keycode) {
            switch (keycode) {
				case Input.Keys.P: 
					paused = !paused;
					break;
				case Input.Keys.T: 
					renderTree = !renderTree; 
					break;
				case Input.Keys.ESCAPE: 
					Gdx.app.exit();
					break;
			}
            return false;
        }

        @Override
        public boolean keyUp(int keycode) {
            return cameraController.keyUp(keycode);
        }

        @Override
        public boolean keyTyped(char character) {
            return cameraController.keyTyped(character);
        }

        @Override
        public boolean touchDown(int screenX, int screenY, int pointer, int button) {		
			return cameraController.touchDown(screenX, screenY, pointer, button);
        }

        @Override
        public boolean touchUp(int screenX, int screenY, int pointer, int button) {
            return cameraController.touchUp(screenX, screenY, pointer, button);
        }

        @Override
        public boolean touchDragged(int screenX, int screenY, int pointer) {
            return cameraController.touchDragged(screenX, screenY, pointer);
        }

        @Override
        public boolean mouseMoved(int screenX, int screenY) {
            return cameraController.mouseMoved(screenX, screenY);
        }

        @Override
        public boolean scrolled(int amount) {
            return cameraController.scrolled(amount);
        }
    }


}
