package com.moniev.verlet.core.MainEngine;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.RecursiveAction;

import com.moniev.verlet.core.Particle.Particle;


public class OuterCollisionTask extends RecursiveAction {
    private final Octree tree;
    private final OctreeNode node;
    private final float subStepDt;
    private final ConcurrentLinkedQueue<CollisionPair> collisionQueue;

    public OuterCollisionTask(Octree octree, OctreeNode node, float subStepDt, ConcurrentLinkedQueue<CollisionPair> collisionQueue) {
        this.tree = octree;
        this.subStepDt = subStepDt;
        this.node = node;
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
            ArrayList<OctreeNode> adjacentNodes = node.getAdjacentNodes(tree.root);
            ArrayList<Particle> outerParticles = tree.getBorderParticles(adjacentNodes);
            for(int i = 0; i < node.particles.size(); i++) {
                for(int j = 0; j < outerParticles.size(); j++) {
                    Particle p1 = node.particles.get(i);
                    Particle p2 = outerParticles.get(j);
                    if(checkCollision(p1, p2)) {
                        collisionQueue.add(new CollisionPair(p1, p2));
                    }
                }
            }
        } else {
            OuterCollisionTask[] tasks = new OuterCollisionTask[node.children.length];
            for (int i = 0; i < node.children.length; i++) {
                tasks[i] = new OuterCollisionTask(tree, node.children[i], subStepDt, collisionQueue);
                tasks[i].fork();
            }

            for (OuterCollisionTask task : tasks) {
                task.join();
            }
        }
    }
}
