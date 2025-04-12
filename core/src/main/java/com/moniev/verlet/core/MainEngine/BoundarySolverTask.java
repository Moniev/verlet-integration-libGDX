package com.moniev.verlet.core.MainEngine;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Task responsible for solving boundary conditions in the Verlet simulation.
 * This task processes the nodes of the octree that represent the boundary
 * of the simulation area. It uses a batch processing approach to handle 
 * multiple nodes in parallel.
 */
public class BoundarySolverTask implements Runnable {
    private final Octree octree; // The Octree that holds all the nodes and particles
    private final float subStepDt; // The time step for the simulation
    private final ConcurrentLinkedQueue<OctreeNode> boundaryQueue; // Queue to hold nodes that are within boundary for further processing
    private static final int BATCHSIZE = 10; // The number of items to process in each batch for efficiency

    /**
     * Constructs a BoundarySolverTask.
     * 
     * @param octree the octree representing the simulation space
     * @param subStepDt the time step used for the simulation substeps
     */
    public BoundarySolverTask(Octree octree, float subStepDt) {
        this.octree = octree;
        this.subStepDt = subStepDt;
        this.boundaryQueue = new ConcurrentLinkedQueue<>(octree.getBorderNodes());
    }

    /**
     * Runs the task, processing the nodes in the boundary queue.
     * It divides the work into batches to avoid processing too many nodes at once.
     */
    @Override
    public void run() {
        while (!boundaryQueue.isEmpty()) {
            ArrayList<OctreeNode> batch = new ArrayList<>(BATCHSIZE);

            for (int i = 0; i < BATCHSIZE; i++) {
                OctreeNode node = boundaryQueue.poll();
                if (node == null) break;
                batch.add(node);
            }

            for (OctreeNode node : batch) {
                octree.resolveBoundary(node, subStepDt);
            }
        }
    }
}
