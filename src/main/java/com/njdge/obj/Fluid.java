package com.njdge.obj;

public class Fluid {
    private final double K;
    private final double n;
    private final double height;
    private final double area;

    public Fluid(double K, double n, double height, double area) {
        this.K = K;
        this.n = n;
        this.height = height;
        this.area = area;
    }

    public double calculateForce(double velo) {
        double shearRate = velo / height;
        double tau = K * Math.pow(shearRate, n);
        return tau * area;
    }
}
