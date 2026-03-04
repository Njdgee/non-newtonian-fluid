package com.njdge;


import com.njdge.obj.Car;

public class Main {

    public final double SPEED_LIMIT = 20.0; //unit: m*s^-1
    public static final double g = 9.80665; //unit: m*s^-2
    public static final double HEIGHT = 0.5; //unit: m
    public static final double AREA = 1; //unit: m^2
    public static double k;
    public static double n = 1.5;


    public static void main(String[] args) {
        Car car = new Car(300, 25);
        double f = AREA * k * Math.pow(car.getSpeed() / HEIGHT, n);




    }
    // Method to check if the acceleration is within the threshold for safe driving (ref: https://imgcdn.cna.com.tw/www/WebPhotos/1024/20191218/2048x2048_448915404793.jpg)
    public static boolean isWithinThreshold (double value) {
        return value >= 0.08 && value <= 0.25; //unit: m*s^-2
    }



}