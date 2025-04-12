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

/**
 * Engine class responsible for simulating and rendering particles in a 3D space using Verlet integration.
 */
public class Engine {
    
    public final ModelBuilder modelBuilder;   // ModelBuilder used to create 3D models.
    public final int particlesLimit;          // Maximum number of particles allowed in the simulation.
    public final int size;                    // Size of the simulation space.
    public final int subSteps;                // Number of sub-steps for each simulation update.
    public float mTime;                       // Time elapsed in the simulation.
    public float mFrameDt;                    // Time per frame for the simulation.
    public Octree tree;                       // Octree structure for spatial partitioning of particles.
    Model sharedModel;                        // Shared model for particle representation.

    /**
     * Constructor for the Engine class.
     * @param particlesLimit Maximum number of particles in the simulation.
     * @param size Size of the simulation space.
     * @param subSteps Number of sub-steps for each simulation update.
     * @param rate Frame rate for the simulation.
     */
    public Engine(int particlesLimit, int size, int subSteps, float rate) {
        Vector center = new Vector(0, 0, 0);
        
        this.subSteps = subSteps;
        this.modelBuilder = new ModelBuilder();
        this.mFrameDt = 1.f / rate;
        this.tree = new Octree(center, size, 16, modelBuilder, mFrameDt);
        this.particlesLimit = particlesLimit;
        this.size = size;
        this.sharedModel = modelBuilder.createSphere(
            1f, 
            1f, 
            1f, 
            64, 
            64, 
            new Material(), 
            Usage.Position | Usage.Normal);
        
    }

    /**
     * Generates a random float value between the specified minimum and maximum values.
     * @param min Minimum value.
     * @param max Maximum value.
     * @return Random float between min and max.
     */
    public float randomFloat(float min, float max) {
        return min + (max - min) * ThreadLocalRandom.current().nextFloat();
    }

    /**
     * Generates a random 3D vector with components between the specified minimum and maximum values.
     * @param min Minimum value for each vector component.
     * @param max Maximum value for each vector component.
     * @return Random 3D vector.
     */
    public Vector randomVector(float min, float max) {
        return new Vector(
            randomFloat(min, max), 
            randomFloat(min, max),  
            randomFloat(min, max)   
        ); 
    }

    /**
     * Adds a particle to the simulation at a calculated position.
     * @param i Index of the particle to calculate its position and velocity.
     */
    public void addParticle(int i) {
        ModelInstance instance = new ModelInstance(sharedModel);  
        instance.materials.get(0).set(calculateColor()); 
        
        Vector position = new Vector(0, size - 2, 0); 
        Particle particle = new Particle(position, 0.5f, 1.f, sharedModel, instance);  
        Vector velocity = calculateCoordinates(i);
        particle.setVelocity(velocity, mFrameDt); 
        tree.addParticle(particle); 
    }

    /**
     * Adds particles to the simulation until the particles limit is reached.
     * @param i Index of the particle to calculate its position and velocity.
     */
    public void addParticles(int i){
        if(tree.countParticles(tree.root) < particlesLimit) { 
            addParticle(i); 
        }
    }

    /**
     * Calculates the position of a particle based on a spiraling pattern.
     * @param i Index of the particle to calculate its position.
     * @return The calculated 3D position of the particle.
     */
    public Vector calculateCoordinates(int i) {
        float turnFraction = (float) ((3 - Math.sqrt(5)) * Math.PI);  // Fraction for golden spiral
        float normIter = (float) i / (particlesLimit - 1);  // Normalized index to range from 0 to 1
        float scale = 10f;  // Scale of the spiral
        float distance = scale * (float) Math.sqrt(normIter);  // Distance from the center based on index
        float angle = 2 * (float) Math.PI * turnFraction * i;  // Angle for the spiral

        float x = distance * (float) Math.cos(angle);  // X coordinate of the particle
        float y = distance * (float) Math.sin(angle);  // Y coordinate of the particle
        float z = scale * (float) Math.sin(normIter * Math.PI);  // Z coordinate of the particle

        return new Vector(x, y, z);  // Return the calculated position as a vector
    }

    /**
     * Calculates the color of a particle based on the current simulation time.
     * @return A Material object with the calculated diffuse color for the particle.
     */
    public Material calculateColor() {
        float r = (float)Math.sin((double)mTime / 4);  // Red component based on time
        float g = (float)Math.sin((double)mTime / 4 + 0.33f * 2.0f * Math.PI);  // Green component based on time
        float b = (float)Math.sin((double)mTime / 4 + 0.66f * 2.0f * Math.PI);  // Blue component based on time
        float a = 1f;  // Alpha component is always 1 for full opacity
        return new Material(ColorAttribute.createDiffuse(r, g, b, a));  // Return the material with the calculated color
    }

    /**
     * Renders all particles in the simulation.
     * @param modelBatch The ModelBatch used for rendering the particles.
     */
    public void renderParticles(ModelBatch modelBatch) {
        tree.renderParticles(modelBatch, tree.root);  
    }

    /**
     * Renders the Octree structure in the simulation.
     * @param modelBatch The ModelBatch used for rendering the Octree.
     */
    public void renderTree(ModelBatch modelBatch) {
        tree.renderTree(modelBatch, tree.root); 
    }
    
    /**
     * Updates the simulation by processing each sub-step, resolving gravity, collisions, and updating particle positions.
     */
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

    /**
     * Disposes of all particles in the simulation.
     */
    public void disposeParticles() {
        tree.disposeParticles(tree.root); 
    }

    /**
     * Disposes of the Octree structure.
     */
    public void disposeTree(){
        tree.disposeTree(tree.root); 
    }
}
