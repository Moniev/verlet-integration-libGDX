package com.moniev.verlet.core.MainEngine;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.moniev.verlet.core.Particle.Particle;
import com.moniev.verlet.core.Vector.Vector;

public class CollisionSolverTask implements Runnable {
    private final ConcurrentLinkedQueue<CollisionPair> collisionQueue;
    private final float subStepDt;
    private static final int batchSize = 10;
    private final float restitution = 0.1f;
    private final float correctionFactor = 0.5f; 

    public CollisionSolverTask(ConcurrentLinkedQueue<CollisionPair> collisionQueue, float subStepDt) {
        this.collisionQueue = collisionQueue;
        this.subStepDt = subStepDt;
    }

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

    @Override
    public void run() {
        while (!collisionQueue.isEmpty()) {
            ArrayList<CollisionPair> batch = new ArrayList<>(batchSize);
            
            for (int i = 0; i < batchSize; i++) {
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