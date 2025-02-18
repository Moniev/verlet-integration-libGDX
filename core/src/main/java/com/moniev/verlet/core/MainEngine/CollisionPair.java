package com.moniev.verlet.core.MainEngine;
import com.moniev.verlet.core.Particle.Particle;


public class CollisionPair {
    Particle p1, p2;
    
    public CollisionPair(Particle p1, Particle p2) {
        this.p1 = p1;
        this.p2 = p2;
    }
}
