package com.moniev.verlet.core.MainEngine;

import com.moniev.verlet.core.Particle.Particle;

/**
 * Represents a pair of particles involved in a collision.
 * This class holds the two particles and provides a way to group them for collision resolution.
 */
public class CollisionPair {
    public Particle p1, p2; // Particles considered in collision pair

    /**
     * Constructs a CollisionPair with the specified particles.
     * 
     * @param p1 the first particle in the collision
     * @param p2 the second particle in the collision
     */
    public CollisionPair(Particle p1, Particle p2) {
        this.p1 = p1;
        this.p2 = p2;
    }
}
