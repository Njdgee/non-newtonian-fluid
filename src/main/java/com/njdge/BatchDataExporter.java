package com.njdge;

import com.njdge.obj.Fluid;
import com.njdge.obj.VehicleModel;
import org.joml.Vector2f;

public class BatchDataExporter {
    public static void main(String[] args) {
        generateCriticalVelocityMatrix();
    }

    public static void generateCriticalVelocityMatrix() {

        //測試的 n 值陣列
        double[] nValues = {1.5, 1.6, 1.7, 1.8, 1.9, 2.0};

        System.out.print("K_Value");
        for (double n : nValues) {
            System.out.print(", n=" + n);
        }
        System.out.println();

        // for k from 10 to 250 with step of 10
        for (int k = 10; k <= 250; k += 10) {
            System.out.print(k);

            // testing for each n value
            for (double n : nValues) {
                float groundY_m = 3.0f;
                Fluid testFluid = new Fluid(k, n, 4.0f, groundY_m + 0.73f, 0.8);
                double criticalVelocity = -1;

                // searching for critical velocity from 1.0 m/s to 30.0 m/s with step of 0.1 m/s
                for (double v0 = 1.0; v0 <= 30.0; v0 += 0.1) {
                    VehicleModel testWheel = new VehicleModel(60, 0.7874/2);
                    testWheel.setPosition(new Vector2f(0, groundY_m - (float)testWheel.getRadius()));
                    testWheel.setVelocity(new Vector2f((float) v0, 0));

                    double currentMaxTotalForce = 0;
                    double elasticAtMax = 0;
                    double dampingAtMax = 0;
                    double simTime = 0.0;

                    // 執行模擬直到車輪壓過減速帶
                    while (testWheel.getPosition().x < 8.5f && simTime < 5.0) {
                        double dt = 0.016 / 10;
                        simTime += dt;

                        Vector2f totalForce = new Vector2f(0, (float) (testWheel.getMass() * 9.81));
                        double totalTorque = 0;

                        Object[] fluidInteraction = testFluid.getInteraction(testWheel);
                        Vector2f fluidForce = (Vector2f) fluidInteraction[0];

                        double currentElastic = 0;
                        double currentDamping = 0;
                        if (fluidInteraction.length >= 4) {
                            currentElastic = (Double) fluidInteraction[2];
                            currentDamping = (Double) fluidInteraction[3];
                        }

                        totalForce.add(fluidForce);
                        totalTorque += (Double) fluidInteraction[1];

                        // Ground contact logic
                        float lowestPointY = testWheel.getPosition().y + (float)testWheel.getRadius();
                        float overlapGround = lowestPointY - groundY_m;

                        if (overlapGround > 0) {
                            double groundNormalForce = 50000 * overlapGround + 2000 * testWheel.getVelocity().y;
                            if (groundNormalForce < 0) groundNormalForce = 0;

                            float contactVx = testWheel.getVelocity().x - (float)(testWheel.getOmega() * testWheel.getRadius());
                            double groundFrictionForce = -1000 * contactVx;
                            double maxFriction = 0.8 * groundNormalForce;

                            if (groundFrictionForce > maxFriction) groundFrictionForce = maxFriction;
                            if (groundFrictionForce < -maxFriction) groundFrictionForce = -maxFriction;

                            totalForce.add(new Vector2f((float)groundFrictionForce, (float)-groundNormalForce));
                            totalTorque += -testWheel.getRadius() * groundFrictionForce;
                        }

                        // record the maximum total force and corresponding elastic/damping at that moment
                        double currentForce = fluidForce.length();
                        if (currentForce > currentMaxTotalForce && testWheel.getPosition().x < 4.8f) {
                            currentMaxTotalForce = currentForce;
                            elasticAtMax = currentElastic;
                            dampingAtMax = currentDamping;
                        }

                        testWheel.update(totalForce, totalTorque, dt);
                    }

                    if (dampingAtMax > elasticAtMax) {
                        criticalVelocity = v0;
                        break;
                    }
                }

                if (criticalVelocity != -1) {
                    System.out.print(", " + criticalVelocity);
                } else {
                    System.out.print(", >30");
                }
            }
            System.out.println();
        }
    }
}