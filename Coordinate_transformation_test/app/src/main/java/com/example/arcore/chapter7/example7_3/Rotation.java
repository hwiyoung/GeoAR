package com.example.arcore.chapter7.example7_3;

public class Rotation {

    private double om;
    private double ph;
    private double kp;

    public Rotation() {

    }

    public void rot3D() {

        double[][] Rx = {{1,0,0},{0,Math.cos(om),-Math.sin(om)},{0,Math.sin(om),Math.cos(om)}};
        double[][] Ry = {{Math.cos(ph),0,Math.sin(ph)},{0,1,0},{-Math.sin(ph),0,Math.cos(ph)}};
        double[][] Rz = {};

    }

}
