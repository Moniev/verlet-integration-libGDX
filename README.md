# Verlet Algorithm with Multithreading and Octree  

This project models the motion of particles in a three-dimensional space with the aid of Verlet integration, which is a numerical method that predicts the object's location over time without keeping track of its velocity; rather, it predicts the next position using the existing and previous positions along with some external force like, for instance, gravity. This technique is more energy efficient and numerically stable than simple Euler integration, hence more applicable for physics simulations. Because tracking the interaction of numerous particles can be costly in terms of computation, the simulation uses an Octree to efficiently manage spatial relationships. The simulation space is divided into progressively smaller cubic zones containing nearby particles. This enables the system to easily search for neighboring particles and also lowers the number of distance calculations required for collision detection and force computations. Without this optimization, the cost of computation would increase with the square power of the number of particles, thus making large simulations impractical.

## Examples 
![Simulation Image](https://imgur.com/Hhu2Ma3.jpg)
![Simulation Image](https://imgur.com/GIL6cbW.jpg)

Even further, this project employs multithreading to improve efficiency since multiple computational tasks can be done on different cores of the CPU. Instead of processing each particle one after the other, the system runs parallel updates to the positions and interactions of the particles, allowing for a faster simulation step time. This means that the system can manage a larger number of particles while still functioning at real time.

## Features  
- **Multithreading**: Parallel computation for improved performance.  
- **Octree-based spatial partitioning**: Reduces complexity of neighbor searches.  
- **Stable and accurate Verlet integration**: Ensures smooth motion without numerical instability.
- **Resolving smoothly over 8k objects.**

## Installation  

- **Installation of libGDX is needed to build Maven project.**
- **Clone the libgdx-maven-archetype repository:**
  - `git clone git://github.com/libgdx/libgdx-maven-archetype.git`
  - `cd libgdx-maven-archetype`
  - `mvn install`
- **Clone your project repository:**
  - `git clone https://github.com/Moniev/verlet-integration-libGDX`
  - `cd verlet-integration-libGDX`
- **Run the Maven clean and install command:**
  - `mvn clean install -Pdesktop`
- **After the above steps, you should be able to run the program with:**
  - `mvn integration-test -Pdesktop`  

