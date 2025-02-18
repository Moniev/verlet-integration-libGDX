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

public class Octree {
    public OctreeNode root;
    private final Vector gravity;

    public final int maxDepth = 3;
    public int totalDepth = 1;
    private final float restitution = 0.1f; 
    private final float dampingCoefficient = 0.25f; 
    private final float correctionFactor = 0.2f;

    public final float stepDt;
    public final Vector center;
    public final float minX, minY, minZ;
    public final float maxX, maxY, maxZ;

    private final ForkJoinPool pool;
    public final ExecutorService executor;

    private final ConcurrentLinkedQueue<CollisionPair> innerCollisionQueue;
    private final ConcurrentLinkedQueue<CollisionPair> outerCollisionQueue;
    private final ConcurrentLinkedQueue<CollisionPair> boundaryCollisionQueue;

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
        this.gravity = new Vector(0, -600f, 0);    
        this.pool = new ForkJoinPool();
        this.executor = Executors.newCachedThreadPool();
        this.innerCollisionQueue = new ConcurrentLinkedQueue<>();
        this.outerCollisionQueue = new ConcurrentLinkedQueue<>();
        this.boundaryCollisionQueue = new ConcurrentLinkedQueue<>();
    }

    public void resolveMousePush(Vector position){
        ArrayList<OctreeNode> nearNodes = getNearNodes(position);
        for(OctreeNode node: nearNodes) {
            for(Particle particle : node.particles) {
                if(particle.position.distance(position) <= 10) particle.accelerateToward(position);
            }
        }
    }

    private ArrayList<OctreeNode> getNearNodes(Vector position){
        ArrayList<OctreeNode> nearNodes = new ArrayList<>();
        findNearNodes(nearNodes, root, position);
        return nearNodes;
    }

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

    public ArrayList<OctreeNode> getLeafs() {
        ArrayList<OctreeNode> leafs = new ArrayList<>();
        collectLeafs(root, leafs);
        return leafs;
    }

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

    public void addParticle(Particle particle) {
        root.insert(particle);
    }

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

    public void reflectVelocity(Particle particle, Vector normal, float subStepDt) {
        Vector velocity = particle.getVelocity(subStepDt); 
        
        float velocityNormal = velocity.dotProduct(normal);
        if(velocityNormal >= 0) return;

        Vector reflected = normal.multiply(2 * velocityNormal * particle.mass);
        Vector newVelocity = velocity.substract(reflected).multiply(restitution);
    
        Vector dampedVelocity = newVelocity.multiply(dampingCoefficient);
        particle.setVelocity(dampedVelocity, subStepDt);
    }

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

    public void resolveInnerCollisionsParallel(float subStepDt) {
        if(root == null) return;

        CollisionTask task = new CollisionTask(this, root, subStepDt, innerCollisionQueue);
        pool.invoke(task); 
        
        int numTasks = Math.min(innerCollisionQueue.size() / 10 + 1, 4);
        for (int i = 0; i < numTasks; i++) {
            executor.execute(new CollisionSolverTask(this, innerCollisionQueue, subStepDt));
        }
    }

    public void resolveOuterCollisionsParallel(float subStepDt) {
        if(root == null) return;

        OuterCollisionTask task = new OuterCollisionTask(this, root, subStepDt, outerCollisionQueue);
        pool.invoke(task); 
        
        int numTasks = Math.min(outerCollisionQueue.size() / 10 + 1, 4);
        for (int i = 0; i < numTasks; i++) {
            executor.execute(new CollisionSolverTask(this, outerCollisionQueue, subStepDt));
        }
    }

    public void resolveInnerCollisions(OctreeNode root, float subStepDt) {
        if (root == null) return; 

        if(root.isLeaf) {
            int objects = root.particles.size();
            for(int i = 0; i < objects; i++) {
                for(int j = i + 1; j < objects; j++) {
                    Particle p1 = root.particles.get(i);
                    Particle p2 = root.particles.get(j);
                    if(checkCollision(p1, p2)){
                        resolveCollision(p1, p2, subStepDt);
                    }
                }
            }
        } else {
            for(OctreeNode node : root.children) {
                resolveInnerCollisions(node, subStepDt);
            }
        }
    }

    public void resolveOuterCollisions(OctreeNode root, float subStepDt){
        if(root == null) return;

        if(root.isLeaf) {
            ArrayList<OctreeNode> adjacentNodes = root.getAdjacentNodes(this.root);
            ArrayList<Particle> outerParticles = getBorderParticles(adjacentNodes);
            for(int i = 0; i < root.particles.size(); i++) {
                for(int j = 0; j < outerParticles.size(); j++) {
                    Particle p1 = root.particles.get(i);
                    Particle p2 = outerParticles.get(j);
                    if(checkCollision(p1, p2)) {
                        resolveCollision(p1, p2, subStepDt);
                    }
                }
            }
        } else {
            for(OctreeNode node : root.children) {
                resolveOuterCollisions(node, subStepDt);
            }
        }
    }

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

    public boolean checkCollision(Particle p1, Particle p2) {
        float dx = p1.position.x - p2.position.x;
        float dy = p1.position.y - p2.position.y;
        float dz = p1.position.z - p2.position.z;
        float distanceSquared = dx * dx + dy * dy + dz * dz;
        float radiusSum = p1.radius + p2.radius;
        return distanceSquared <= (radiusSum * radiusSum);
    }

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

    public OctreeNode findTargetNode(OctreeNode root, Particle particle) {    
        if (root.isLeaf) return root;
        int i = root.getChildIndex(particle.position);
        return findTargetNode(root.children[i], particle);
    }
    
}
