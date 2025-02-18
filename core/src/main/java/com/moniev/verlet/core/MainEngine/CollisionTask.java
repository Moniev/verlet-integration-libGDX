package com.moniev.verlet.core.MainEngine;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.RecursiveAction;

import com.moniev.verlet.core.Particle.Particle;



public class CollisionTask extends RecursiveAction {
    private final OctreeNode node;
    private final Octree octree;
    private final float subStepDt;
    private final ConcurrentLinkedQueue<CollisionPair> collisionQueue;

    public CollisionTask(Octree octree, OctreeNode node, float subStepDt, ConcurrentLinkedQueue<CollisionPair> collisionQueue) {
        this.node = node;
        this.subStepDt = subStepDt;
        this.octree = octree;
        this.collisionQueue = collisionQueue;
    }

    public boolean checkCollision(Particle p1, Particle p2) {
        float dx = p1.position.x - p2.position.x;
        float dy = p1.position.y - p2.position.y;
        float dz = p1.position.z - p2.position.z;
        float distanceSquared = dx * dx + dy * dy + dz * dz;
        float radiusSum = p1.radius + p2.radius;
        return distanceSquared <= (radiusSum * radiusSum);
    }

    @Override
    protected void compute() {
        if(node == null) return;

        if(node.isLeaf) {
            int size = node.particles.size();
            for (int i = 0; i < size; i++) {
                for (int j = i + 1; j < size; j++) {
                    Particle p1 = node.particles.get(i);
                    Particle p2 = node.particles.get(j);
                    if (checkCollision(p1, p2)) {
                        collisionQueue.add(new CollisionPair(p1, p2));
                    }
                }
            }
        } else {
            CollisionTask[] tasks = new CollisionTask[node.children.length];
            for (int i = 0; i < node.children.length; i++) {
                tasks[i] = new CollisionTask(octree, node.children[i], subStepDt, collisionQueue);
                tasks[i].fork();
            }

            for (CollisionTask task : tasks) {
                task.join();
            }
        }
    }

}
