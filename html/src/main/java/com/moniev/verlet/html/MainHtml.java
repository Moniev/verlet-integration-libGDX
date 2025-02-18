package com.moniev.verlet.html;

import com.moniev.verlet.core.Main;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;

public class MainHtml extends GwtApplication {
	@Override
	public ApplicationListener getApplicationListener () {
		return new Main();
	}
	
	@Override
	public GwtApplicationConfiguration getConfig () {
		return new GwtApplicationConfiguration(480, 320);
	}
}
