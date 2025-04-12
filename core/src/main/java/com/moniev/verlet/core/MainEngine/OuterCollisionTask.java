package com.moniev.verlet.core.MainEngine;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.RecursiveAction;

import com.moniev.verlet.core.Particle.Particle;

/**
 * The OuterCollisionTask is a recursive task that checks for collisions between particles in an octree.
 * It traverses the octree, checking for collisions between particles within each node and particles from adjacent nodes.
 */
public class OuterCollisionTask extends RecursiveAction {

    private final Octree tree;  // The octree structure used for spatial partitioning in collision detection and other space-related operations.
    private final OctreeNode node;  // The specific octree node associated with the current context, representing a partition of the space.
    private final float subStepDt;  // The time step used for sub-steps in simulations, allowing finer control over physics or collision updates.
    private final ConcurrentLinkedQueue<CollisionPair> collisionQueue;  // A thread-safe queue holding pairs of objects that are in collision, processed for resolution.

    /**
     * Constructor for creating an OuterCollisionTask.
     *
     * @param octree The octree used for particle partitioning.
     * @param node The octree node where collision checking is performed.
     * @param subStepDt The time step used for the simulation.
     * @param collisionQueue The queue to store collision pairs for processing.
     */
    public OuterCollisionTask(Octree octree, OctreeNode node, float subStepDt, ConcurrentLinkedQueue<CollisionPair> collisionQueue) {
        this.tree = octree;
        this.subStepDt = subStepDt;
        this.node = node;
        this.collisionQueue = collisionQueue;
    }

    /**
     * Checks if two particles are colliding based on their positions and radii.
     *
     * @param p1 The first particle.
     * @param p2 The second particle.
     * @return True if the particles are colliding, false otherwise.
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
     * This method is invoked to execute the task. It checks for collisions within the given octree node.
     * If the node is a leaf node, it compares the particles within the node with those from adjacent nodes.
     * If the node is not a leaf node, it recursively divides the task into smaller sub-tasks for each child node.
     */
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
