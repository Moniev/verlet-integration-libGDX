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

/**
 * OctreeNode represents a node in an octree used for spatial partitioning of particles.
 * It can subdivide into 8 child nodes when the number of particles exceeds a limit.
 */
public class OctreeNode {
    
    public final int particlesLimit, depthLimit;  // The limit on the number of particles a node can hold and the maximum depth of the octree node

    public final float minX, minY, minZ;  // Minimum coordinates (X, Y, Z) defining the lower bounds of the octree node
    public final float maxX, maxY, maxZ;  // Maximum coordinates (X, Y, Z) defining the upper bounds of the octree node

    private final float size, halfSize, quarterSize;  // The size of the node and its subdivisions (half and quarter size for efficient calculations)

    public final Vector center;  // The center point of the octree node, representing the average position of all particles within it

    private final Octree tree;  // Reference to the octree structure that this node belongs to
    public OctreeNode parent;  // The parent node in the octree, linking back to the higher level
    public OctreeNode[] children;  // The child nodes in the octree, each representing a subdivision of the space
    public ArrayList<Particle> particles;  // List of particles contained within this node

    public boolean isLeaf, isBorder;  // Boolean flags: isLeaf indicates if this node has no children (leaf node), isBorder indicates if it's near the boundary of the space

    public final Model model;  // The 3D model representation of the octree node for visualization or rendering
    public final ModelInstance modelInstance;  // An instance of the model used for rendering in the scene
    private final ModelBuilder modelBuilder;  // A builder used to construct the 3D model for the octree node

    /**
     * Constructs an OctreeNode with the specified parameters.
     * 
     * @param center The center of the node.
     * @param size The size of the node.
     * @param particlesLimit The maximum number of particles that can be held by this node.
     * @param depthLimit The maximum depth this node can have.
     * @param parent The parent of this node in the octree.
     * @param modelBuilder The model builder used to construct the node's 3D model.
     * @param tree The octree structure this node belongs to.
     */
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

    /**
     * Checks if this node is on the boundary of the octree.
     * 
     * @return true if the node is on the boundary, false otherwise.
     */
    private boolean isBorder() {
        return !(
            minX <= tree.minX || maxX >= tree.maxX ||
            minY <= tree.minY || maxY >= tree.maxY ||
            minZ <= tree.minZ || maxZ >= tree.maxZ
        );
    }

    /**
     * Checks if this node is empty (no particles).
     * 
     * @return true if the node contains no particles, false otherwise.
     */
    public boolean isEmpty() {
        return particles.isEmpty();
    }

    /**
     * Creates a wireframe model of the octree node for visualization.
     * 
     * @return The 3D model representing the octree node.
     */
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

    /**
     * Creates a model instance to represent this node in the 3D scene.
     * 
     * @return The model instance representing the octree node.
     */
    private ModelInstance createModelInstance() {
        ModelInstance newModelInstance = new ModelInstance(model);
        newModelInstance.transform.setToTranslation(
            center.x,
            center.y,
            center.z
        );
        return newModelInstance;
    }

    /**
     * Inserts a particle into this node. If the number of particles exceeds the limit, 
     * the node will subdivide and redistribute the particles among the children.
     * 
     * @param particle The particle to insert into the node.
     */
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
    
    /**
     * Subdivides this node into 8 child nodes.
     */
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

    /**
     * Redistributes the particles of this node to its child nodes.
     */
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

    /**
     * Returns the index of the child node for the given position.
     * 
     * @param position The position to determine the child index for.
     * @return The index of the child node (0 to 7).
     */
    public int getChildIndex(Vector position) {
        int index = 0;
        if (position.x >= center.x) index |= 1;
        if (position.y >= center.y) index |= 2;
        if (position.z >= center.z) index |= 4;
        return index;
    }    

    /**
     * Checks if this node is adjacent to another octree node.
     * 
     * @param node The octree node to check for adjacency.
     * @return true if the nodes are adjacent, false otherwise.
     */
    public boolean isAdjacent(OctreeNode node) {
        double sizeThis = maxX - minX;
        double sizeOther = node.maxX - node.minX;
        if (sizeThis != sizeOther) {
            return false; 
        }

        boolean touchX = maxX == node.minX || minX == node.maxX;
        boolean touchY = maxY == node.minY || minY == node.maxY;
        boolean touchZ = maxZ == node.minZ || minZ == node.maxZ;

        boolean overlapX = maxX >= node.minX && minX <= node.maxX;
        boolean overlapY = maxY >= node.minY && minY <= node.maxY;
        boolean overlapZ = maxZ >= node.minZ && minZ <= node.maxZ;

        return (touchX && overlapY && overlapZ) ||
            (touchY && overlapX && overlapZ) ||
            (touchZ && overlapX && overlapY);
    }

    /**
     * Checks if a given particle is near the border of this node.
     * A particle is considered near the border if its distance to the node's boundary 
     * is less than a specified margin.
     * 
     * @param particle The particle to check.
     * @return true if the particle is near the border of this node, false otherwise.
     */
    public boolean isNearBorder(Particle particle) {
        float margin = particle.radius * 1.25f;
        
        return (
            particle.position.x - margin < minX || particle.position.x + margin > maxX ||
            particle.position.y - margin < minY || particle.position.y + margin > maxY ||
            particle.position.z - margin < minZ || particle.position.z + margin > maxZ
        );
    }

    /**
     * Retrieves all adjacent octree nodes for a given root node.
     * This method searches through the octree structure to find nodes that are adjacent 
     * to the current node.
     * 
     * @param root The root node of the octree to start the search from.
     * @return A list of adjacent octree nodes.
     */
    public ArrayList<OctreeNode> getAdjacentNodes(OctreeNode root) {
        ArrayList<OctreeNode> adjacentNodes = new ArrayList<>();
        findAdjacentNodes(root, adjacentNodes);
        return adjacentNodes;
    }

    /**
     * Recursively searches for adjacent octree nodes starting from the given root node.
     * This method adds adjacent nodes to the provided list of adjacent nodes.
     * 
     * @param root The root node of the octree to start the search from.
     * @param adjacentNodes The list to store the adjacent nodes.
     */
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
