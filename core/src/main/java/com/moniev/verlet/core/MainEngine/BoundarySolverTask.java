package com.moniev.verlet.core.MainEngine;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BoundarySolverTask implements Runnable {
    private final Octree octree;
    private final float subStepDt;
    private final ConcurrentLinkedQueue<OctreeNode> boundaryQueue;
    private static final int batchSize = 10; 

    public BoundarySolverTask(Octree octree, float subStepDt) {
        this.octree = octree;
        this.subStepDt = subStepDt;
        this.boundaryQueue = new ConcurrentLinkedQueue<>(octree.getBorderNodes());
    }

    @Override
    public void run() {
        while (!boundaryQueue.isEmpty()) {
            ArrayList<OctreeNode> batch = new ArrayList<>(batchSize);

            for (int i = 0; i < batchSize; i++) {
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
