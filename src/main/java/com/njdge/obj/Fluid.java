package com.njdge.obj;

import lombok.Data;
import org.joml.Vector2f;
@Data
public class Fluid {
    private final double K; // 稠度係數
    private final double n; // 流動指數
    private final double frictionCoeff = 0.3;
    private final double k0 = 14715;   // 初始基礎剛度 (N/m)
    private final Vector2f position; // 中心點 (公尺)
    private final double radius;     // 半徑 (公尺)

    public Fluid(double K, double n, float x, float y, double radius) {
        this.K = K;
        this.n = n;
        this.position = new Vector2f(x, y);
        this.radius = radius;
    }

    // 回傳 Object 陣列：[0] 是 Vector2f 合力, [1] 是 Double 力矩
    public Object[] getInteraction(VehicleModel vehicle) {
        Vector2f ballPos = vehicle.getPosition();
        Vector2f diff = new Vector2f(position).sub(ballPos); // 從球心指向減速丘中心
        float dist = diff.length();

        // Penetration Depth
        float overlap = (float) (this.radius + vehicle.getRadius()) - dist;

        // 若未接觸，回傳零力與零力矩
        if (overlap <= 0) return new Object[]{new Vector2f(0, 0), 0.0};

        // 1. 建立局部座標系
        Vector2f normal = new Vector2f(ballPos).sub(position).normalize(); // 法向量：從減速丘推向球心
        Vector2f tangent = new Vector2f(-normal.y, normal.x); // 切向量 (逆時針轉90度)

        // 2. 計算接觸點的真實速度 (平移速度 + 旋轉切線速度)
        // r向量：從球心指向接觸點
        float rX = -normal.x * (float)vehicle.getRadius();
        float rY = -normal.y * (float)vehicle.getRadius();

        // 2D 外積計算 v_contact = v + omega x r (螢幕座標系 Y 軸向下，順時針旋轉 omega 為正)
        float contactVx = vehicle.getVelocity().x - (float)vehicle.getOmega() * rY;
        float contactVy = vehicle.getVelocity().y + (float)vehicle.getOmega() * rX;

        float vn = contactVx * normal.x + contactVy * normal.y;
        float vt = contactVx * tangent.x + contactVy * tangent.y;

        // 計算接觸面積 (近似值)
        double contactArea = 2 * Math.sqrt(2 * vehicle.getRadius() * overlap);

        // (a) 計算非牛頓流體剪應力(related to velocity)
        float compressionSpeed = Math.max(0, -vn);
        double nonNewtonianDamping = K * Math.pow(compressionSpeed, n);

        // (b) 計算外殼的非線性彈性恢復力 (related to overlap)
        double elasticForce = k0 * overlap;

        // (c) 總法向力 = 彈性力 + 阻尼力
        double fn = elasticForce + nonNewtonianDamping;
        if (fn < 0) fn = 0;

        // (a) 定義剪切層厚度 dy (使用 overlap，並給予 0.01m 的下限防止除以零)
        double dy = Math.max(0.00001, overlap);

        //(Shear Rate): du/dy
        double shearRate = -vt / dy;

        // η = k * |γ'|^(n-1)
        double apparentViscosity = K * Math.pow(Math.abs(shearRate), n - 1);

        // τ = η * γ'
        double tau = apparentViscosity * shearRate;


        double ft = tau * contactArea;

        double maxFriction = frictionCoeff * fn;
        if (ft > maxFriction) ft = maxFriction;
        if (ft < -maxFriction) ft = -maxFriction;

        // 5. 將力轉換回全局 XY 座標
        Vector2f force = new Vector2f(
                (float)(fn * normal.x + ft * tangent.x),
                (float)(fn * normal.y + ft * tangent.y)
        );


        // 6. 計算力矩 (Torque) = r x F
        double torque = rX * force.y - rY * force.x;

        return new Object[]{force, torque, elasticForce, nonNewtonianDamping};
    }

}