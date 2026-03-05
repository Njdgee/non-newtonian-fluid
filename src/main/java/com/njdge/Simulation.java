package com.njdge;


import com.njdge.obj.Fluid;
import com.njdge.obj.VehicleModel;

public class Simulation {
    public double simulate(double velocity) {
        double dt = 0.001;

        double simulationTime = 1.0;

        VehicleModel car = new VehicleModel(300);
        Fluid fluid = new Fluid(50, 1.8, 0.02, 0.03);
        double maxAcceleration = 0;

        for (double t = 0; t < simulationTime; t+=dt) {
            double force = fluid.calculateForce(velocity);
            car.update(force, dt);
            double acceleration = (force - car.getMass() * car.getGravity()) / car.getMass();
            if (acceleration > maxAcceleration) {
                maxAcceleration = acceleration;
            }
        }
        return maxAcceleration;
    }

    public static void main(String[] args) {
        for (int i = 0; i <= 100; i++) {
            double velocity = i * 0.1;
            Simulation sim = new Simulation();
            double maxAcceleration = sim.simulate(velocity);
            System.out.printf("Velocity: %.1f m/s, Max Acceleration: %.4f m/s^2, Safe: %b%n",
                    velocity, maxAcceleration, isWithinThreshold(maxAcceleration));
        }



    }
    // Method to check if the acceleration is within the threshold for safe driving (ref: https://imgcdn.cna.com.tw/www/WebPhotos/1024/20191218/2048x2048_448915404793.jpg)
    public static boolean isWithinThreshold (double value) {
        return value >= 0.08 && value <= 0.25; //unit: m*s^-2
    }



}