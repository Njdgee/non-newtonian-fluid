package com.njdge.obj;

import lombok.Data;
import org.joml.Vector2f;
@Data
public class VehicleModel {
    private final double mass;
    private final double radius;
    private final double I; // 轉動慣量 (實心圓柱 I = 0.5 * m * r^2)

    private Vector2f position = new Vector2f(0, 0);
    private Vector2f velocity = new Vector2f(0, 0);

    private double angle = 0; // 旋轉角度 (弧度)
    private double omega = 0; // 角速度 (弧度/秒)

    public VehicleModel(double mass, double radius) {
        this.mass = mass;
        this.radius = radius;
        this.I = 0.5 * mass * Math.pow(radius, 2);
    }

    public void update(Vector2f totalForce, double totalTorque, double dt) {
        // 平移運動 (Newton's Second Law)
        Vector2f acceleration = new Vector2f(totalForce).div((float) mass);
        velocity.add(new Vector2f(acceleration).mul((float) dt));
        position.add(new Vector2f(velocity).mul((float) dt));

        // 旋轉運動 (Euler's Equation)
        double angularAcceleration = totalTorque / I;
        omega += angularAcceleration * dt;
        angle += omega * dt;
    }

}