package com.moniev.verlet.core.Particle;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.moniev.verlet.core.Vector.Vector;

public class Particle {
    public Vector position, lastPosition, acceleration; 
    public float mass;
    public float radius;
    public Model model;
    public ModelInstance modelInstance;

    public Particle(Vector position, float radius, float mass, Model model, ModelInstance modelInstance) {
        this.radius = radius;
        this.position = new Vector(position);
        this.lastPosition = new Vector(position); 
        this.mass = mass;
        this.model = model;
        this.modelInstance = modelInstance;
        this.acceleration = new Vector(0, 0, 0);
    }

    public void accelerateToward(Vector target) {
        float dx = target.x - position.x;
        float dy = target.y - position.y;
        float dz = target.z - position.z;
        float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        Vector accelerationNormalized = new Vector(dx / distance, dy / distance, dz / distance);
        accelerate(accelerationNormalized);
    }

    public void accelerate(Vector a) {
        acceleration.set(acceleration.add(a)); 
    }

    public void setVelocity(Vector v, float dt) {
        lastPosition.set(position.substract(v.multiply(dt))); 
    }

    public void addVelocity(Vector v, float dt) {
        lastPosition.set(lastPosition.substract(v.multiply(dt))); 
    }   

    public Vector getVelocity(float dt) { 
        return position.substract(lastPosition).subdivide(dt); 
    } 

    public void printParticle() {
        String sPosition = position.toString();
        System.out.printf("[position: %s][mass %f]\n", sPosition, mass);
    }

    public void update(float dt) {        
        Vector displacement = position.substract(lastPosition);        
        Vector newPosition = position.add(displacement).add(acceleration.multiply(dt * dt).multiply(0.27f));
    
        lastPosition.set(position); 
        position.set(newPosition); 

        acceleration.set(0); 
    }
}
