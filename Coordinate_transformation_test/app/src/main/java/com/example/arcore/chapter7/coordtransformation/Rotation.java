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

    public double[] TransformG2L(double[][] R, double[] Location_Org, double[] Location_GPS) {
        double[] translation = new double[3];
        for (int i = 0; i < Location_Org.length; i++) {
            translation[i] = Location_Org[i] - Location_GPS[i];
        }

        double[] result = new double[3];

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                result[i] += R[i][j] * translation[j];
            }
        }

        return result;
    }
}
