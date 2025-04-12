package com.moniev.verlet.core.MainEngine;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;

import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.moniev.verlet.core.Particle.Particle;
import com.moniev.verlet.core.Vector.Vector;

/**
 * The Octree class represents a spatial partitioning structure used to organize particles
 * in 3D space for efficient collision detection and physics simulation.
 * It contains methods for adding particles, resolving collisions, and updating their positions.
 */
public class Octree {
    
    public OctreeNode root;  // The root node of the octree, containing all subdivisions of the 3D space
    private final Vector gravity;  // The gravity vector applied to all particles, usually pointing downward

    public final int maxDepth = 3;  // The maximum depth of the octree, limiting its subdivisions
    public int totalDepth = 1;  // The current depth of the octree, starts at 1 and increases as the tree subdivides
    private final float restitution = 0.1f;  // Coefficient determining energy retained after a collision (bounce)
    private final float dampingCoefficient = 0.25f;  // Coefficient for damping (resistance to movement) applied to particles

    public final float stepDt;  // The time step (delta time) for each physics update step
    public final Vector center;  // The center point of the octree in 3D space, used for calculating boundaries
    public final float minX, minY, minZ;  // Minimum coordinates (X, Y, Z) defining the lower bounds of the octree
    public final float maxX, maxY, maxZ;  // Maximum coordinates (X, Y, Z) defining the upper bounds of the octree

    private final ForkJoinPool pool;  // A thread pool used for parallel computations (like physics updates)
    public final ExecutorService executor;  // Executor service for managing task execution in a concurrent environment

    private final ConcurrentLinkedQueue<CollisionPair> innerCollisionQueue;  // Queue for collisions detected within the octree boundaries
    private final ConcurrentLinkedQueue<CollisionPair> outerCollisionQueue;  // Queue for collisions detected outside the octree boundaries
    private final ConcurrentLinkedQueue<CollisionPair> boundaryCollisionQueue;  // Queue for collisions at the boundary of the octree space

    /**
     * Constructs an Octree with the specified parameters.
     *
     * @param center The center of the octree
     * @param size The size of the octree's boundary
     * @param threads The number of threads for parallel execution
     * @param modelBuilder A model builder for particle visualization
     * @param stepDt The time step for particle updates
     */
    public Octree(Vector center, int size, int threads, ModelBuilder modelBuilder, float stepDt) {
        this.center = center;
        this.minX = center.x - size;
        this.minY = center.y - size;
        this.minZ = center.z - size;

        this.maxX = center.x + size;
        this.maxY = center.y + size;
        this.maxZ = center.z + size;

        this.stepDt = stepDt;

        this.root = new OctreeNode(center, size, size, maxDepth, null, modelBuilder, this); 
        this.gravity = new Vector(0, -1000f, 0);    
        this.pool = new ForkJoinPool();
        this.executor = Executors.newCachedThreadPool();
        this.innerCollisionQueue = new ConcurrentLinkedQueue<>();
        this.outerCollisionQueue = new ConcurrentLinkedQueue<>();
        this.boundaryCollisionQueue = new ConcurrentLinkedQueue<>();
    }

     /**
     * Resolves a mouse push event by accelerating nearby particles toward the given position.
     *
     * @param position The position of the mouse push
     */
    public void resolveMousePush(Vector position){
        ArrayList<OctreeNode> nearNodes = getNearNodes(position);
        for(OctreeNode node: nearNodes) {
            for(Particle particle : node.particles) {
                if(particle.position.distance(position) <= 10) particle.accelerateToward(position);
            }
        }
    }

     /**
     * Returns a list of octree nodes that are near the given position.
     *
     * @param position The position to check
     * @return A list of nearby octree nodes
     */
    private ArrayList<OctreeNode> getNearNodes(Vector position){
        ArrayList<OctreeNode> nearNodes = new ArrayList<>();
        findNearNodes(nearNodes, root, position);
        return nearNodes;
    }

     /**
     * Recursively finds nodes near the given position and adds them to the provided list.
     *
     * @param nearNodes The list to add found nodes to
     * @param root The current node to check
     * @param position The position to check against
     */
    private void findNearNodes(ArrayList<OctreeNode> nearNodes, OctreeNode root, Vector position) {
        if(root == null) return;
        
        if(root.isLeaf && !root.particles.isEmpty()){
            if(root.center.distance(position) <= 10) {
                nearNodes.add(root);
            }
        } else {
            for(OctreeNode node : root.children) {
                findNearNodes(nearNodes, node, position);
            }
        }
    }

    /**
     * Counts the number of particles in the given octree node and its children.
     *
     * @param root The node to count particles in
     * @return The total number of particles
     */
    public int countParticles(OctreeNode root) {
        if (root == null) return 0;
    
        int count = 0;
        if (root.isLeaf) {
            count += root.particles.size(); 
        } else {

            for (OctreeNode child : root.children) {
                count += countParticles(child); 
            }
        }
    
        return count; 
    }

     /**
     * Returns a list of all leaf nodes in the octree.
     *
     * @return A list of leaf nodes
     */
    public ArrayList<OctreeNode> getLeafs() {
        ArrayList<OctreeNode> leafs = new ArrayList<>();
        collectLeafs(root, leafs);
        return leafs;
    }

     /**
     * Recursively collects leaf nodes in the provided list.
     *
     * @param node The current node to check
     * @param leafNodes The list to collect leaf nodes in
     */
    public void collectLeafs(OctreeNode node, ArrayList<OctreeNode> leafNodes) {
        if (node == null) return;

        if (node.isLeaf) {
            leafNodes.add(node);
        } else {
            for (OctreeNode child : node.children) {
                collectLeafs(child, leafNodes);
            }
        }
    }

    /**
     * Adds a particle to the octree.
     *
     * @param particle The particle to add
     */
    public void addParticle(Particle particle) {
        root.insert(particle);
    }

     /**
     * Updates the positions of all particles in the octree.
     *
     * @param root The root node of the octree
     * @param subStepDt The time step for the update
     */
    public void updateParticles(OctreeNode root, float subStepDt) {
        if (root == null) return; 
        
        if(root.isLeaf) {
            for(Particle particle : root.particles) {
                particle.update(subStepDt);
            }
        } else {
            for(OctreeNode node : root.children) {
                updateParticles(node, subStepDt);
            }
        }
    }

    /**
     * Resolves collisions with the boundary of the octree.
     *
     * @param root The root node of the octree
     * @param subStepDt The time step for the collision resolution
     */
    public void resolveBoundary(OctreeNode root, float subStepDt) {
        if (root == null) return; 
    
        if (root.isLeaf) {
            for (Particle particle : root.particles) {
                boolean reflected = false; 
    
                if (particle.position.x - particle.radius <= minX) {
                    Vector normal = new Vector(1, 0, 0);
                    reflectVelocity(particle, normal, subStepDt);
                    particle.position.x = minX + particle.radius;
                    reflected = true;
                } else if (particle.position.x + particle.radius >= maxX) {
                    Vector normal = new Vector(-1, 0, 0); 
                    reflectVelocity(particle, normal, subStepDt);
                    particle.position.x = maxX - particle.radius;
                    reflected = true;
                }
                    
                if (particle.position.y - particle.radius <= minY) {
                    Vector normal = new Vector(0, 1, 0);
                    reflectVelocity(particle, normal, subStepDt);
                    particle.position.y = minY + particle.radius;
                    reflected = true;
                } else if (particle.position.y + particle.radius >= maxY) {
                    Vector normal = new Vector(0, -1, 0);
                    reflectVelocity(particle, normal, subStepDt);
                    particle.position.y = maxY - particle.radius;
                    reflected = true;
                }
        
                if (particle.position.z - particle.radius <= minZ) {
                    Vector normal = new Vector(0, 0, 1);
                    reflectVelocity(particle, normal, subStepDt);
                    particle.position.z = minZ + particle.radius;
                    reflected = true;
                } else if (particle.position.z + particle.radius >= maxZ) {
                    Vector normal = new Vector(0, 0, -1);
                    reflectVelocity(particle, normal, subStepDt);
                    particle.position.z = maxZ - particle.radius;
                    reflected = true;
                }
    
                if (reflected) {
                    particle.acceleration.set(0);
                    particle.accelerate(gravity);
                }
            }
    
        } else {
            for (OctreeNode node : root.children) {
                resolveBoundary(node, subStepDt);
            }
        }
    }

    public void resolveBoundaryParallel(float subStepDt){
        int numTasks = Math.min(boundaryCollisionQueue.size() / 10 + 1, 4);
        for (int i = 0; i < numTasks; i++) {
            executor.execute(new BoundarySolverTask(this, subStepDt));
        }
    }

    /**
     * Reflects a particle's velocity upon collision with the boundary.
     *
     * @param particle The particle to reflect
     * @param normal The normal vector of the collision surface
     * @param subStepDt The time step for the velocity update
     */
    public void reflectVelocity(Particle particle, Vector normal, float subStepDt) {
        Vector velocity = particle.getVelocity(subStepDt);
        float velocityNormal = velocity.dotProduct(normal);
    

        if (velocityNormal >= 0) return;
    
        Vector reflected = normal.multiply(2 * velocityNormal);
        Vector newVelocity = velocity.substract(reflected).multiply(restitution);
    
        newVelocity = newVelocity.multiply(dampingCoefficient);
        particle.setVelocity(newVelocity, subStepDt);
    }

    /**
     * Resolves the gravity effect on all particles in the octree.
     *
     * @param root The root node of the octree
     */
    public void resolveGravity(OctreeNode root) {
        if (root == null) return; 
        
        if(root.isLeaf) {
            for(Particle particle : root.particles) {
                particle.accelerate(gravity);
            }
        } else {
            for(OctreeNode node : root.children) {
                resolveGravity(node);
            }
        }
    }

    /**
     * Resolves inner collisions between particles in the octree in parallel.
     *
     * @param subStepDt The time step for collision resolution
     */
    public void resolveInnerCollisionsParallel(float subStepDt) {
        if(root == null) return;

        CollisionTask task = new CollisionTask(this, root, subStepDt, innerCollisionQueue);
        pool.invoke(task); 
        
        int numTasks = Math.min(innerCollisionQueue.size() / 10 + 1, 4);
        for (int i = 0; i < numTasks; i++) {
            executor.execute(new CollisionSolverTask(innerCollisionQueue, subStepDt));
        }
    }

    /**
     * Resolves outer collisions between particles in the octree in parallel.
     *
     * @param subStepDt The time step for collision resolution
     */
    public void resolveOuterCollisionsParallel(float subStepDt) {
        if(root == null) return;

        OuterCollisionTask task = new OuterCollisionTask(this, root, subStepDt, outerCollisionQueue);
        pool.invoke(task); 
        
        int numTasks = Math.min(outerCollisionQueue.size() / 10 + 1, 4);
        for (int i = 0; i < numTasks; i++) {
            executor.execute(new CollisionSolverTask(outerCollisionQueue, subStepDt));
        }
    }

    /**
     * Retrieves the border particles from the given adjacent nodes.
     * 
     * @param adjacentNodes The adjacent nodes to check.
     * @return A list of border particles.
     */
    public ArrayList<Particle> getBorderParticles(ArrayList<OctreeNode> adjacentNodes) {
        ArrayList<Particle> borderParticles = new ArrayList<>();
        for(OctreeNode node : adjacentNodes) {
            for(Particle particle : node.particles) {
                if(node.isNearBorder(particle)) {
                    borderParticles.add(particle);
                }
            }
        }
        return borderParticles;
    }

    /**
     * Retrieves the border nodes from the octree.
     * 
     * @return A list of border nodes.
     */
    public ArrayList<OctreeNode> getBorderNodes() {
        ArrayList<OctreeNode> borderNodes = new ArrayList<>();
        findBorderNodes(root, borderNodes);
        return borderNodes;
    }

    private void findBorderNodes(OctreeNode root, ArrayList<OctreeNode> borderNodes) {
        if(root == null) return;

        if(root.isLeaf && root.isBorder) {
            borderNodes.add(root);
        }

        for(OctreeNode node : root.children) {
            findBorderNodes(node, borderNodes);
        }
    }

    /**
     * Checks if two particles are colliding.
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
     * Renders the particles in the octree.
     * 
     * @param modelBatch The ModelBatch used for rendering.
     * @param root The root node to start rendering from.
     */
    public void renderParticles(ModelBatch modelBatch, OctreeNode root) {    
        if (root == null) return; 
        
        if(root.isLeaf) {
            for(Particle particle : root.particles) {
                particle.modelInstance.transform.setToTranslation(
                    particle.position.x, 
                    particle.position.y, 
                    particle.position.z
                );
                modelBatch.render(particle.modelInstance);  
            }
        } else {
            for(OctreeNode node : root.children) {
                renderParticles(modelBatch, node);
            }
        }
    }

    /**
     * Disposes the particles in the octree.
     * 
     * @param root The root node to start disposing from.
     */
    public void disposeParticles(OctreeNode root) {
        if (root == null) return; 

        if(root.isLeaf) {
            for(Particle particle : root.particles) {
                particle.model.dispose();
            }
        } else {
            for(OctreeNode node : root.children) {
                disposeParticles(node);
            }
        }
    }

    /**
     * Disposes the octree tree structure.
     * 
     * @param root The root node to start disposing from.
     */
    public void disposeTree(OctreeNode root) {
        if (root == null) return; 

        if(root.isLeaf) {
            root.model.dispose();
        } else {
            for(OctreeNode node : root.children) {
                disposeTree(node);
            }
        }
    }

    /**
     * Renders the entire octree tree.
     * 
     * @param modelBatch The ModelBatch used for rendering.
     * @param root The root node to start rendering from.
     */
    public void renderTree(ModelBatch modelBatch, OctreeNode root) {
        if (root == null) return; 

        if(root.isLeaf) {
            modelBatch.render(root.modelInstance);
        } else {
            for(OctreeNode node : root.children){
                renderTree(modelBatch, node);
            }
        }
    }


    /**
     * Updates the spatial lookup for particles in the octree.
     * 
     * @param node The node to start the spatial lookup update from.
     */
    public void updateSpatialLookup(OctreeNode node) {
        if (node == null) return;
    
        if (node.isLeaf) {
            ArrayList<Particle> particlesToMove = new ArrayList<>();
            for (Particle particle : node.particles) {
                OctreeNode target = findTargetNode(root, particle);
                if (target != node) { 
                    particlesToMove.add(particle);
                }
            }
            
            for (Particle particle : particlesToMove) {
                node.particles.remove(particle); 
                OctreeNode target = findTargetNode(root, particle);
                if (target != null) {
                    target.insert(particle); 
                }
            }

        } else {
            for (OctreeNode child : node.children) {
                updateSpatialLookup(child);
            }
        }
    }

    /**
     * Finds the target node for a given particle.
     * 
     * @param root The root node to start searching from.
     * @param particle The particle to find the target node for.
     * @return The target node for the particle.
     */
    public OctreeNode findTargetNode(OctreeNode root, Particle particle) {    
        if (root.isLeaf) return root;
        int i = root.getChildIndex(particle.position);
        return findTargetNode(root.children[i], particle);
    }
    
}
