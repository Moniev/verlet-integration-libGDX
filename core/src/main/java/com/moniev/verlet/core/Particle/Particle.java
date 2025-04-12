package com.moniev.verlet.core.Particle;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.moniev.verlet.core.Vector.Vector;

/**
 * Represents a particle in the simulation with position, velocity, and other physical properties.
 */
public class Particle {
    
    public Vector position, lastPosition, acceleration;  // Position, last position, and acceleration vectors of the particle.
    public float mass;  // The mass of the particle.
    public float radius;  // The radius of the particle, used for collision detection or visual representation.
    public Model model;  // The 3D model representing the particle (for visualization purposes).
    public ModelInstance modelInstance;  // The instance of the 3D model for rendering in the scene.

    /**
     * Constructor to initialize a particle with specific physical properties.
     * 
     * @param position Initial position of the particle.
     * @param radius The radius of the particle.
     * @param mass The mass of the particle.
     * @param model The model representing the particle.
     * @param modelInstance The instance of the model used for rendering.
     */
    public Particle(Vector position, float radius, float mass, Model model, ModelInstance modelInstance) {
        this.radius = radius;
        this.position = new Vector(position);
        this.lastPosition = new Vector(position); 
        this.mass = mass;
        this.model = model;
        this.modelInstance = modelInstance;
        this.acceleration = new Vector(0, 0, 0);
    }

    /**
     * Accelerates the particle toward a target position.
     * 
     * @param target The target position to accelerate the particle towards.
     */
    public void accelerateToward(Vector target) {
        float dx = target.x - position.x;
        float dy = target.y - position.y;
        float dz = target.z - position.z;
        float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        Vector accelerationNormalized = new Vector(dx / distance, dy / distance, dz / distance);
        accelerate(accelerationNormalized);
    }

    /**
     * Adds an acceleration vector to the particle's current acceleration.
     * 
     * @param a The acceleration vector to be added.
     */
    public void accelerate(Vector a) {
        acceleration.set(acceleration.add(a)); 
    }

    /**
     * Sets the particle's velocity based on a given velocity vector and time delta.
     * 
     * @param v The velocity vector.
     * @param dt The time delta.
     */
    public void setVelocity(Vector v, float dt) {
        lastPosition.set(position.substract(v.multiply(dt))); 
    }

    /**
     * Adds velocity to the particle's last position based on a given velocity vector and time delta.
     * 
     * @param v The velocity vector.
     * @param dt The time delta.
     */
    public void addVelocity(Vector v, float dt) {
        lastPosition.set(lastPosition.substract(v.multiply(dt))); 
    }   

    /**
     * Calculates and returns the particle's velocity based on the current and last positions.
     * 
     * @param dt The time delta.
     * @return The velocity of the particle.
     */
    public Vector getVelocity(float dt) { 
        return position.substract(lastPosition).subdivide(dt); 
    } 

    /**
     * Prints out the current position and mass of the particle.
     */
    public void printParticle() {
        String sPosition = position.toString();
        System.out.printf("[position: %s][mass %f]\n", sPosition, mass);
    }

    /**
     * Updates the particle's position based on its velocity and acceleration.
     * This method is typically called on each simulation step.
     * 
     * @param dt The time step for the simulation.
     */
    public void update(float dt) {        
        Vector displacement = position.substract(lastPosition);        
        Vector newPosition = position.add(displacement).add(acceleration.multiply(dt * dt).multiply(0.27f));
    
        lastPosition.set(position); 
        position.set(newPosition); 

        acceleration.set(0); 
    }
}
