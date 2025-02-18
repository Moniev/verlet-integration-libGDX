package com.moniev.verlet.core.MainEngine;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.moniev.verlet.core.Particle.Particle;

public class CollisionSolverTask implements Runnable {
    private final Octree octree;
    private final ConcurrentLinkedQueue<CollisionPair> collisionQueue;
    private final float subStepDt;
    private static final int batchSize = 10;
    private final float restitution = 0.1f;
    private final float correctionFactor = 0.2f; 

    public CollisionSolverTask(Octree octree, ConcurrentLinkedQueue<CollisionPair> collisionQueue, float subStepDt) {
        this.octree = octree;
        this.collisionQueue = collisionQueue;
        this.subStepDt = subStepDt;
    }

    public void resolveCollision(Particle p1, Particle p2, float subStepDt) {
        float dx = p2.position.x - p1.position.x;
        float dy = p2.position.y - p1.position.y;
        float dz = p2.position.z - p1.position.z;
        
        double dDistance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        float distance = (float) dDistance;
        if (distance == 0) return;
    
        float radiusSum = p1.radius + p2.radius;
        if (distance < radiusSum) {
            float overlap = radiusSum - distance;
            float nx = dx / distance;
            float ny = dy / distance;
            float nz = dz / distance;

            p1.position.x -= (overlap / 2) * nx;
            p1.position.y -= (overlap / 2) * ny;
            p1.position.z -= (overlap / 2) * nz;

            p2.position.x += (overlap / 2) * nx;
            p2.position.y += (overlap / 2) * ny;
            p2.position.z += (overlap / 2) * nz;
        }

        float nx = dx / distance;
        float ny = dy / distance;
        float nz = dz / distance;

        float vx = p1.getVelocity(subStepDt).x - p2.getVelocity(subStepDt).x;
        float vy = p1.getVelocity(subStepDt).y - p2.getVelocity(subStepDt).y;
        float vz = p1.getVelocity(subStepDt).z - p2.getVelocity(subStepDt).z;
    
        float dotProduct = vx * nx + vy * ny + vz * nz;
        float massSum = p1.mass + p2.mass;

        if (dotProduct > 0) return;
    
        float impulse = ((2 * dotProduct) / massSum);
    
        p1.getVelocity(subStepDt).x -= (impulse * p2.mass * nx) * restitution;
        p1.getVelocity(subStepDt).y -= (impulse * p2.mass * ny) * restitution;
        p1.getVelocity(subStepDt).z -= (impulse * p2.mass * nz) * restitution;
    
        p2.getVelocity(subStepDt).x += (impulse * p1.mass * nx) * restitution;
        p2.getVelocity(subStepDt).y += (impulse * p1.mass * ny) * restitution;
        p2.getVelocity(subStepDt).z += (impulse * p1.mass * nz) * restitution;

        float overlap = (p1.radius + p2.radius - distance) / 2.0f;
        if (overlap > 0) {
            float correction = (overlap * correctionFactor) / massSum;

            p1.position.x -= (correction * overlap * nx);
            p1.position.y -= (correction * overlap * ny);
            p1.position.z -= (correction * overlap * nz);
    
            p2.position.x += (correction * overlap * nx);
            p2.position.y += (correction * overlap * ny);
            p2.position.z += (correction * overlap * nz);
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