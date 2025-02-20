package com.moniev.verlet.core.Vector;

import com.badlogic.gdx.math.Vector3;

public class Vector {
    public float x, y, z;

    public Vector(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector(float[] vector) {
        this.x = vector[0];
        this.y = vector[1];
        this.z = vector[2];
    }

    public Vector(Vector other) {
        this.x = other.x;
        this.y = other.y;
        this.z = other.z;
    }

    public Vector(Vector3 other) {
        this.x = other.x;
        this.y = other.y;
        this.z = other.z;
    }

    public Vector add(Vector other) {
        return new Vector(this.x + other.x, this.y + other.y, this.z + other.z);
    }

    public Vector substract(Vector other) {
        return new Vector(this.x - other.x, this.y - other.y, this.z - other.z);
    }

    public Vector substract(float scalar) {
        return new Vector(this.x - scalar, this.y - scalar, this.z - scalar);
    }

    public Vector subdivide(float scalar) {
        return new Vector(this.x / scalar, this.y / scalar, this.z / scalar);
    }

    public Vector clamp(float maxLength) {
        float length = this.length();
        if (length > maxLength) {
            return subdivide(length).multiply(maxLength);
        }
        return new Vector(this);
    }
    

    public Vector crossProduct(Vector other) {
        return new Vector(
            this.y * other.z - other.y * this.z, 
            -(this.x * other.z - other.x * this.z), 
            this.x * other.y - other.x * this.y
        );
    }

    public Vector multiply(float scalar) {
        return new Vector(this.x * scalar, this.y * scalar, this.z * scalar);
    }

    public float dotProduct(Vector other) {
        return this.x * other.x + this.y * other.y + this.z * other.z;
    }

    public void set(Vector other) {
        this.x = other.x;
        this.y = other.y;
        this.z = other.z;
    }

    public void set(float scalar) {
        this.x = scalar;
        this.y = scalar;
        this.z = scalar;
    }

    public float length() {
        return (float) Math.sqrt(Math.pow(this.x, 2) + Math.pow(this.y, 2) + Math.pow(this.z, 2));
    }

    public float distance(Vector other) {
        return (float) Math.sqrt(Math.pow(this.x - other.x, 2) + Math.pow(this.y - other.y, 2) + Math.pow(this.z - other.z, 2));
    }

    public float distance(float x, float y, float z) {
        return (float) Math.sqrt(Math.pow(this.x - x, 2) + Math.pow(this.y - y, 2) + Math.pow(this.z - z, 2));
    }

    public String toString() {
        return "x: " + this.x + "y: " + this.y + "z: " + this.z;
    }

    public void print(){
        System.out.println(this.toString());
    }

}
