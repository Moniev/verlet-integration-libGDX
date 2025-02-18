package com.moniev.verlet.core.MainEngine;

import java.util.concurrent.ThreadLocalRandom;

import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.moniev.verlet.core.Particle.Particle;
import com.moniev.verlet.core.Vector.Vector;

public class Engine {
    public final ModelBuilder modelBuilder;
    public final int particlesLimit;
    public final int size;
    public final int subSteps;
    public float mTime;
    public float mFrameDt;
    public Octree tree;


    public Engine(int particlesLimit, int size, int subSteps, float rate) {
        Vector center = new Vector(0, 0, 0);
        
        this.subSteps = subSteps;
        this.modelBuilder = new ModelBuilder();
        this.mFrameDt = 1.f / rate;
        this.tree = new Octree(center, size, 16, modelBuilder, mFrameDt);
        this.particlesLimit = particlesLimit;
        this.size = size;
    }

    public float randomFloat(float min, float max) {
        return min + (max - min) * ThreadLocalRandom.current().nextFloat();
    }

    public Vector randomVector(float min, float max) {
        return new Vector(
            randomFloat(min, max),
            randomFloat(min, max),
            randomFloat(min, max)
        ); 
    }

    public void addParticle(int i) {
        Model particleModel = modelBuilder.createSphere(
            1f, 1f, 1f, 64, 64,
            calculateColor(),
            Usage.Position | Usage.Normal
        );
        ModelInstance modelInstance = new ModelInstance(particleModel);
        
        Vector position = new Vector(0, size - 2, 0);
        Particle particle = new Particle(position, 0.5f, 1.f, particleModel, modelInstance);
        Vector velocity = calculateCoordinates(i); 
        particle.setVelocity(velocity, mFrameDt);
        tree.addParticle(particle);
    }

    public void addParticles(int i){
        if(tree.countParticles(tree.root) < particlesLimit) {
            addParticle(i);
        }
    }

    public Vector calculateCoordinates(int i) {
        float turnFraction = (float) ((3 - Math.sqrt(5)) * Math.PI);
        float normIter = (float) i / (particlesLimit - 1);
        float scale = 10f; 
        float distance = scale * (float) Math.sqrt(normIter); 
        float angle = 2 * (float) Math.PI * turnFraction * i;

        float x = distance * (float) Math.cos(angle);
        float y = distance * (float) Math.sin(angle);
        float z = scale * (float) Math.sin(normIter * Math.PI);

        return new Vector(x, y, z);
    }

    public Material calculateColor() {
        float r = (float)Math.sin((double)mTime / 4);
        float g = (float)Math.sin((double)mTime / 4 + 0.33f * 2.0f * Math.PI);
        float b = (float)Math.sin((double)mTime / 4 + 0.66f * 2.0f * Math.PI);
        float a = 0f;
        return new Material(ColorAttribute.createDiffuse(r, g, b, a));
    }

    public void renderParticles(ModelBatch modelBatch) {
        tree.renderParticles(modelBatch, tree.root);
    }

    public void renderTree(ModelBatch modelBatch) {
        tree.renderTree(modelBatch, tree.root);
    }
    
    public void update() {
        float subStepDt = mFrameDt / (float)subSteps;
        mTime += mFrameDt;
        for(int i = 0; i < subSteps; i++) {
            tree.resolveGravity(tree.root);
            tree.resolveBoundaryParallel(subStepDt);
            tree.resolveInnerCollisionsParallel(subStepDt);
            tree.resolveOuterCollisionsParallel(subStepDt);
            tree.updateParticles(tree.root, subStepDt);
            tree.updateSpatialLookup(tree.root);
        }
    }

    public void disposeParticles() {
        tree.disposeParticles(tree.root);
    }

    public void disposeTree(){
        tree.disposeTree(tree.root);
    }
}
