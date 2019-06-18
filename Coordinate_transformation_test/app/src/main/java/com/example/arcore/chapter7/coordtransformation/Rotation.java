package com.example.arcore.chapter7.coordtransformation;

public class Rotation {

    public Rotation() {
    }

    public double[][] Rot3D(float om, float ph, float kp){
        double[][] Rx = {{1, 0, 0},
                        {0, Math.cos(om), Math.sin(om)},
                        {0, -Math.sin(om), Math.cos(om)}};
        double[][] Ry = {{Math.cos(ph), 0, -Math.sin(ph)},
                            {0, 1, 0},
                            {Math.sin(ph), 0, Math.cos(ph)}};
        double[][] Rz = {{Math.cos(kp), Math.sin(kp), 0},
                            {-Math.sin(kp), Math.cos(kp), 0},
                            {0, 0, 1}};

        double[][] result_1 = new double[3][3];
        double[][] result_2 = new double[3][3];

        for (int i = 0; i < 3; i++){
            for (int j = 0; j < 3; j++){
                for (int k = 0; k < 3; k++){
                    result_1[i][j] += Ry[i][k]*Rx[k][j];
                }
            }
        }

        for (int i = 0; i < 3; i++){
            for (int j = 0; j < 3; j++){
                for (int k = 0; k < 3; k++){
                    result_2[i][j] += Rz[i][k]*result_1[k][j];
                }
            }
        }
        return result_2;
    }

    public double[] TransformG2L(double[][] R, double[] Location_Anchor, double[] Location_GPS) {
        double[] translation = new double[3];
        for (int i = 0; i < Location_Anchor.length; i++) {
            translation[i] = Location_Anchor[i] - Location_GPS[i];
        }

        double[] result = new double[3];

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                result[i] += R[i][j] * translation[j];
            }
        }

        return result;
    }

    public double[] ConvertA2Q(double azimuth) {    // yaw (Z), pitch (Y), roll (X)
        // yaw (Z)
        double yaw = 0;
        double cy = Math.cos(yaw / 2);
        double sy = Math.sin(yaw / 2);
        // pitch (Y)
        double azimuth2 = -azimuth;     // azimuth - LHS, quaternion - RHS
        double cp = Math.cos(azimuth2 / 2);
        double sp = Math.sin(azimuth2 / 2);
        // roll (X)
        double roll = 0;
        double cr = Math.cos(roll / 2);
        double sr = Math.sin(roll / 2);

        // Conversion
        double w = cy * cp * cr + sy * sp * sr;
        double x = cy * cp * sr - sy * sp * cr;
        double y = sy * cp * sr + cy * sp * cr;
        double z = sy * cp * cr - cy * sp * sr;

        double[] q = new double[4];
        q[0] = x; q[1] = y; q[2] = z; q[3] = w;
        return q;
    }
}
