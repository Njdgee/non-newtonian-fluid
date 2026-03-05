package com.njdge.obj;

import lombok.Data;

@Data
public class VehicleModel {
//    private final double speed; //unit: m*s^-1
    private final double mass; //unit: kg
    private final double gravity = 9.80665; //unit: m*s^-2
    private double v_y; //unit: m*s^-2
    private double y; //unit: m

    public VehicleModel(double mass) {
        this.mass = mass;
    }

    public void update(double force,double dt) {
        double acceleration = (force - mass * gravity) / mass;

        v_y += acceleration * dt; //update velocity
        y += v_y * dt; //update position
    }
}
