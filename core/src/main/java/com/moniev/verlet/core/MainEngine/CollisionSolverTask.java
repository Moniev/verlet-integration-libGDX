package com.moniev.verlet.core.MainEngine;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.moniev.verlet.core.Particle.Particle;
import com.moniev.verlet.core.Vector.Vector;

/**
 * A task that resolves collisions between particles. It processes collision pairs from the queue
 * and resolves the collisions by adjusting the positions and velocities of the particles involved.
 * This task is intended to be executed in a separate thread for concurrency.
 */
public class CollisionSolverTask implements Runnable {
    private final ConcurrentLinkedQueue<CollisionPair> collisionQueue; // Queue containing collision pairs
    private final float subStepDt; // Time step for the simulation
    private static final int BATCHSIZE = 10; // Number of collision pairs to process in each batch
    private final float restitution = 0.1f; // Coefficient of restitution for collision resolution
    private final float correctionFactor = 0.5f; // Correction factor for particle position adjustment

    /**
     * Constructs a CollisionSolverTask with the specified collision queue and time step.
     * 
     * @param collisionQueue the queue of collision pairs to process
     * @param subStepDt the time step for each simulation substep
     */
    public CollisionSolverTask(ConcurrentLinkedQueue<CollisionPair> collisionQueue, float subStepDt) {
        this.collisionQueue = collisionQueue;
        this.subStepDt = subStepDt;
    }

    /**
     * Resolves a collision between two particles by adjusting their positions and velocities.
     * This includes calculating overlap, correcting positions, and applying an impulse for velocity adjustment.
     * 
     * @param p1 the first particle involved in the collision
     * @param p2 the second particle involved in the collision
     * @param subStepDt the time step for the simulation
     */
    public void resolveCollision(Particle p1, Particle p2, float subStepDt) {
        Vector delta = p2.position.substract(p1.position);
        float distance = delta.length();
        
        if (distance == 0) {
            delta = new Vector(
                (float) (Math.random() * 0.01f), 
                (float) (Math.random() * 0.01f), 
                (float) (Math.random() * 0.01f)
            );
            distance = delta.length();
        }
    
        float radiusSum = p1.radius + p2.radius;
    
        if (distance <= radiusSum) {
            Vector normal = delta.subdivide(distance);
            float overlap = radiusSum - distance;
    
            float massSum = p1.mass + p2.mass;
            float correction = (overlap * correctionFactor) / massSum;
    
            p1.position = p1.position.substract(normal.multiply(correction * p2.mass));
            p2.position = p2.position.add(normal.multiply(correction * p1.mass));

            Vector relativeVelocity = p1.getVelocity(subStepDt).substract(p2.getVelocity(subStepDt));
            float velocityAlongNormal = relativeVelocity.dotProduct(normal);
    
            if (velocityAlongNormal > 0) return;
    
            float impulseMagnitude = -(1 + restitution) * velocityAlongNormal / massSum;
            Vector impulse = normal.multiply(impulseMagnitude);
    
            p1.setVelocity(p1.getVelocity(subStepDt).add(impulse.multiply(p2.mass)).multiply(0.9f), subStepDt);
            p2.setVelocity(p2.getVelocity(subStepDt).substract(impulse.multiply(p1.mass)).multiply(0.9f), subStepDt);
        }
    }

    /**
     * Processes the collision queue by resolving collisions in batches.
     * Each batch processes a set of collision pairs to avoid processing too many at once.
     */
    @Override
    public void run() {
        while (!collisionQueue.isEmpty()) {
            ArrayList<CollisionPair> batch = new ArrayList<>(BATCHSIZE);
            
            for (int i = 0; i < BATCHSIZE; i++) {
                CollisionPair pair = collisionQueue.poll();
                if (pair == null) break;
                batch.add(pair);
            }

            for (CollisionPair pair : batch) {
                resolveCollision(pair.p1, pair.p2, subStepDt);
            }
        }
    }
}
