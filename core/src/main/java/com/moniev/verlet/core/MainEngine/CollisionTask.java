package com.moniev.verlet.core.MainEngine;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.RecursiveAction;

import com.moniev.verlet.core.Particle.Particle;

/**
 * A task that checks for collisions between particles within an Octree node and adds collision pairs
 * to the collision queue. This task can operate recursively on child nodes of the Octree to detect
 * collisions throughout the entire spatial partition.
 */
public class CollisionTask extends RecursiveAction {
    private final OctreeNode node; // The current node of the Octree being processed
    private final Octree octree; // The Octree that holds all nodes and particles
    private final float subStepDt; // The time step for the simulation
    private final ConcurrentLinkedQueue<CollisionPair> collisionQueue; // The queue to hold detected collision pairs

    /**
     * Constructs a CollisionTask for a specific node in the Octree.
     * 
     * @param octree the Octree that holds all the particles
     * @param node the Octree node to process
     * @param subStepDt the time step for the simulation
     * @param collisionQueue the queue to add collision pairs
     */
    public CollisionTask(Octree octree, OctreeNode node, float subStepDt, ConcurrentLinkedQueue<CollisionPair> collisionQueue) {
        this.node = node;
        this.subStepDt = subStepDt;
        this.octree = octree;
        this.collisionQueue = collisionQueue;
    }

    /**
     * Checks whether two particles are colliding based on their positions and radii.
     * 
     * @param p1 the first particle
     * @param p2 the second particle
     * @return true if the particles are colliding, false otherwise
     */
    public boolean checkCollision(Particle p1, Particle p2) {
        float dx = p1.position.x - p2.position.x;
        float dy = p1.position.y - p2.position.y;
        float dz = p1.position.z - p2.position.z;
        float distanceSquared = dx * dx + dy * dy + dz * dz;
        float radiusSum = p1.radius + p2.radius;
        return distanceSquared <= (radiusSum * radiusSum);
    }

    /**
     * Recursively checks for collisions within the Octree. If the node is a leaf node, it checks
     * all particles within that node for collisions. If the node is not a leaf, it recursively processes
     * its child nodes.
     */
    @Override
    protected void compute() {
        if (node == null) return;

        if (node.isLeaf) {
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
