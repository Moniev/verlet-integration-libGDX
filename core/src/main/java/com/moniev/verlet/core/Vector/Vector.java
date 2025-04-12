package com.moniev.verlet.core.Vector;

import com.badlogic.gdx.math.Vector3;

public class Vector {
    public float x, y, z; // The coordinates of the vector in 3D space (x, y, z).

    /**
     * Constructor for a Vector object with specific x, y, and z values.
     * @param x The x-coordinate of the vector.
     * @param y The y-coordinate of the vector.
     * @param z The z-coordinate of the vector.
     */
    public Vector(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Constructor for a Vector object from an array of floats.
     * @param vector Array containing the x, y, and z coordinates of the vector.
     */
    public Vector(float[] vector) {
        this.x = vector[0];
        this.y = vector[1];
        this.z = vector[2];
    }

    /**
     * Copy constructor for creating a new Vector from another Vector.
     * @param other The Vector object to copy.
     */
    public Vector(Vector other) {
        this.x = other.x;
        this.y = other.y;
        this.z = other.z;
    }

    /**
     * Constructor for creating a Vector from a libGDX Vector3 object.
     * @param other The Vector3 object to convert into a Vector.
     */
    public Vector(Vector3 other) {
        this.x = other.x;
        this.y = other.y;
        this.z = other.z;
    }

    /**
     * Adds another vector to this vector.
     * @param other The vector to add.
     * @return A new vector resulting from the addition.
     */
    public Vector add(Vector other) {
        return new Vector(this.x + other.x, this.y + other.y, this.z + other.z);
    }

    /**
     * Subtracts another vector from this vector.
     * @param other The vector to subtract.
     * @return A new vector resulting from the subtraction.
     */
    public Vector substract(Vector other) {
        return new Vector(this.x - other.x, this.y - other.y, this.z - other.z);
    }

    /**
     * Subtracts a scalar value from each component of this vector.
     * @param scalar The scalar value to subtract.
     * @return A new vector resulting from the scalar subtraction.
     */
    public Vector substract(float scalar) {
        return new Vector(this.x - scalar, this.y - scalar, this.z - scalar);
    }

    /**
     * Divides each component of the vector by a scalar value.
     * @param scalar The scalar value to divide by.
     * @return A new vector resulting from the division.
     */
    public Vector subdivide(float scalar) {
        return new Vector(this.x / scalar, this.y / scalar, this.z / scalar);
    }

    /**
     * Clamps the vector to a maximum length.
     * @param maxLength The maximum length for the vector.
     * @return A new vector clamped to the specified maximum length.
     */
    public Vector clamp(float maxLength) {
        float length = this.length();
        if (length > maxLength) {
            return subdivide(length).multiply(maxLength);
        }
        return new Vector(this);
    }

    /**
     * Computes the cross product of this vector and another vector.
     * @param other The other vector to compute the cross product with.
     * @return A new vector representing the cross product.
     */
    public Vector crossProduct(Vector other) {
        return new Vector(
            this.y * other.z - other.y * this.z, 
            -(this.x * other.z - other.x * this.z), 
            this.x * other.y - other.x * this.y
        );
    }

    /**
     * Multiplies this vector by a scalar.
     * @param scalar The scalar value to multiply the vector by.
     * @return A new vector resulting from the multiplication.
     */
    public Vector multiply(float scalar) {
        return new Vector(this.x * scalar, this.y * scalar, this.z * scalar);
    }

    /**
     * Computes the dot product of this vector and another vector.
     * @param other The other vector to compute the dot product with.
     * @return The scalar value resulting from the dot product.
     */
    public float dotProduct(Vector other) {
        return this.x * other.x + this.y * other.y + this.z * other.z;
    }

    /**
     * Sets this vector to have the same components as another vector.
     * @param other The vector whose values will be copied.
     */
    public void set(Vector other) {
        this.x = other.x;
        this.y = other.y;
        this.z = other.z;
    }

    /**
     * Sets all components of this vector to a single scalar value.
     * @param scalar The scalar value to assign to all components of the vector.
     */
    public void set(float scalar) {
        this.x = scalar;
        this.y = scalar;
        this.z = scalar;
    }

    /**
     * Computes the length (magnitude) of the vector.
     * @return The length of the vector.
     */
    public float length() {
        return (float) Math.sqrt(Math.pow(this.x, 2) + Math.pow(this.y, 2) + Math.pow(this.z, 2));
    }

    /**
     * Computes the distance between this vector and another vector.
     * @param other The other vector to compute the distance to.
     * @return The distance between the two vectors.
     */
    public float distance(Vector other) {
        return (float) Math.sqrt(Math.pow(this.x - other.x, 2) + Math.pow(this.y - other.y, 2) + Math.pow(this.z - other.z, 2));
    }

    /**
     * Computes the distance between this vector and a point represented by (x, y, z).
     * @param x The x-coordinate of the point.
     * @param y The y-coordinate of the point.
     * @param z The z-coordinate of the point.
     * @return The distance between the vector and the point.
     */
    public float distance(float x, float y, float z) {
        return (float) Math.sqrt(Math.pow(this.x - x, 2) + Math.pow(this.y - y, 2) + Math.pow(this.z - z, 2));
    }

    /**
     * Returns a string representation of the vector.
     * @return A string describing the vector's components.
     */
    @Override
    public String toString() {
        return "x: " + this.x + " y: " + this.y + " z: " + this.z;
    }

    /**
     * Prints the string representation of the vector to the console.
     */
    public void print(){
        System.out.println(this.toString());
    }
}
