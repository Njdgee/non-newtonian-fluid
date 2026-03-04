package com.njdge.obj;

import lombok.Data;

@Data
public class Car {
    private double speed; //unit: m*s^-1
    private double a_y; //unit: m*s^-2
    private double mass; //unit: kg
    private double x,y; //unit: m

    public Car(double mass, double speed) {
        this.mass = mass;
        this.speed = speed;
        this.x = 0.0;
        this.y = 0.0;
    }





}
