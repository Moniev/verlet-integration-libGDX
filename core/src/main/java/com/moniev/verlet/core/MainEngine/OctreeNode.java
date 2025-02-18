package com.moniev.verlet.core.MainEngine;

import java.util.ArrayList;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.moniev.verlet.core.Particle.Particle;
import com.moniev.verlet.core.Vector.Vector;


public class OctreeNode {
    public final int particlesLimit, depthLimit;
    
    public final float minX, minY, minZ;
    public final float maxX, maxY, maxZ;

    private final float size, halfSize, quarterSize;

    public final Vector center;

    private Octree tree;
    public OctreeNode parent;
    public OctreeNode[] children;
    public ArrayList<Particle> particles;

    public boolean isLeaf, isBorder;

    public final Model model;
    public final ModelInstance modelInstance;
    private final ModelBuilder modelBuilder;

    public OctreeNode(Vector center, float size, int particlesLimit, int depthLimit, OctreeNode parent, ModelBuilder modelBuilder, Octree tree) {
        this.tree = tree;
        this.parent = parent;
        this.particlesLimit = particlesLimit;
        this.depthLimit = depthLimit;
        this.modelBuilder = new ModelBuilder();

        this.center = center;
        this.size = size;
        
        this.particles = new ArrayList<>(); 
        this.children = new OctreeNode[8];

        this.isLeaf = true;

        this.halfSize = this.size / 2f;
        this.quarterSize = this.size / 4f;

        this.minX = center.x - halfSize;
        this.minY = center.y - halfSize;
        this.minZ = center.z - halfSize;

        this.maxX = center.x + halfSize;
        this.maxY = center.y + halfSize;
        this.maxZ = center.z + halfSize;

        this.model = createWireframeModel();
        this.modelInstance = createModelInstance();
        this.isBorder = isBorder();
    }

    private boolean isBorder() {
        return !(
            minX <= tree.minX || maxX >= tree.maxX ||
            minY <= tree.minY || maxY >= tree.maxY ||
            minZ <= tree.minZ || maxZ >= tree.maxZ
        );
    }

    public boolean isEmpty() {
        return particles.isEmpty();
    }

    private Model createWireframeModel() {
        float x = center.x;
        float y = center.y;
        float z = center.z;
        float w = size;
        float h = size;
        float d = size;

        Vector[] vertices = {
            new Vector(x - w, y - h, z - d),
            new Vector(x + w, y - h, z - d),
            new Vector(x + w, y + h, z - d),
            new Vector(x - w, y + h, z - d),
            new Vector(x - w, y - h, z + d),
            new Vector(x + w, y - h, z + d),
            new Vector(x + w, y + h, z + d),
            new Vector(x - w, y + h, z + d)
        };
        modelBuilder.begin();
        MeshPartBuilder builder = modelBuilder.part("line", 1, 3, new Material());
        builder.setColor(Color.GREEN);

        builder.line(vertices[0].x, vertices[0].y, vertices[0].z, vertices[1].x, vertices[1].y, vertices[1].z);
        builder.line(vertices[1].x, vertices[1].y, vertices[1].z, vertices[2].x, vertices[2].y, vertices[2].z);
        builder.line(vertices[2].x, vertices[2].y, vertices[2].z, vertices[3].x, vertices[3].y, vertices[3].z);
        builder.line(vertices[3].x, vertices[3].y, vertices[3].z, vertices[0].x, vertices[0].y, vertices[0].z);
        
        builder.line(vertices[4].x, vertices[4].y, vertices[4].z, vertices[5].x, vertices[5].y, vertices[5].z);
        builder.line(vertices[5].x, vertices[5].y, vertices[5].z, vertices[6].x, vertices[6].y, vertices[6].z);
        builder.line(vertices[6].x, vertices[6].y, vertices[6].z, vertices[7].x, vertices[7].y, vertices[7].z);
        builder.line(vertices[7].x, vertices[7].y, vertices[7].z, vertices[4].x, vertices[4].y, vertices[4].z);
        
        builder.line(vertices[0].x, vertices[0].y, vertices[0].z, vertices[4].x, vertices[4].y, vertices[4].z);
        builder.line(vertices[1].x, vertices[1].y, vertices[1].z, vertices[5].x, vertices[5].y, vertices[5].z);
        builder.line(vertices[2].x, vertices[2].y, vertices[2].z, vertices[6].x, vertices[6].y, vertices[6].z);
        builder.line(vertices[3].x, vertices[3].y, vertices[3].z, vertices[7].x, vertices[7].y, vertices[7].z);

        return modelBuilder.end();
    }   

    private ModelInstance createModelInstance() {
        ModelInstance newModelInstance = new ModelInstance(model);
        newModelInstance.transform.setToTranslation(
            center.x,
            center.y,
            center.z
        );
        return newModelInstance;
    }

    public void insert(Particle particle) {
        if (isLeaf) {
            particles.add(particle);
    
            if (particles.size() > particlesLimit && depthLimit > 0) {
                subdivide();
                redistributeParticles();
            }
        } else {
            if (children[0] == null) {
                subdivide();
            }
            int i = getChildIndex(particle.position);
            children[i].insert(particle);
        }
    }
    

    public void subdivide() {
        for(int i = 0; i < 8; i++) {
            float xOffSet = ((i & 1) == 0) ? -quarterSize : quarterSize;
            float yOffSet = ((i & 2) == 0) ? -quarterSize : quarterSize;
            float zOffSet = ((i & 4) == 0) ? -quarterSize : quarterSize;

            Vector childCenter = new Vector(
                center.x + xOffSet, 
                center.y + yOffSet,  
                center.z + zOffSet
            );
            children[i] = new OctreeNode(childCenter, halfSize, particlesLimit, depthLimit - 1, this, modelBuilder, this.tree);
        }
        isLeaf = false; 
    }

    public void redistributeParticles() {
        ArrayList<Particle> temp = new ArrayList<>(particles);
        particles.clear();
    
        for(Particle particle : temp) {
            int i = getChildIndex(particle.position);
            children[i].insert(particle);
        }

        if (particles.size() > particlesLimit) {
            redistributeParticles();
        }
    } 

    public int getChildIndex(Vector position) {
        int index = 0;
        if (position.x >= center.x) index |= 1;
        if (position.y >= center.y) index |= 2;
        if (position.z >= center.z) index |= 4;
        return index;
    }    

    public boolean isAdjacent(OctreeNode node) {
        boolean overlapX = maxX >= node.minX && minX <= node.maxX;
        boolean overlapY = maxY >= node.minY && minY <= node.maxY;
        boolean overlapZ = maxZ >= node.minZ && minZ <= node.maxZ;
        return overlapX && overlapY && overlapZ;
    }

    public boolean isNearBorder(Particle particle) {
        float margin = particle.radius * 1.25f;
    
        return (
            particle.position.x - margin < minX || particle.position.x + margin > maxX ||
            particle.position.y - margin < minY || particle.position.y + margin > maxY ||
            particle.position.z - margin < minZ || particle.position.z + margin > maxZ
        );
    }
    

    public ArrayList<OctreeNode> getAdjacentNodes(OctreeNode root) {
        ArrayList<OctreeNode> adjacentNodes = new ArrayList<>();
        findAdjacentNodes(root, adjacentNodes);
        return adjacentNodes;
    }

    public void findAdjacentNodes(OctreeNode root, ArrayList<OctreeNode> adjacentNodes) {
        if (root == null) return;

        if(root.isLeaf && isAdjacent(root) && this != root) {
            adjacentNodes.add(root);
        } else if (!root.isLeaf) {
            for(OctreeNode node : root.children) {
                findAdjacentNodes(node, adjacentNodes);
            }
        }
    }
}
