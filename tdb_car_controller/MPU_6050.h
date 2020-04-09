
#pragma once

#include <Wire.h>
#include <Arduino.h>

#define CLOCK_NORMAL 100000
#define CLOCK_FAST 400000

class MPU_6050
{

  public:

    MPU_6050();
    MPU_6050(unsigned int _address, int _sclock);
    void initialize();                // initialize the communication with mpu 6050
    boolean refresh();
    int16_t getAccel(char axis);
    int16_t getTemp();
    int16_t getGyro(char axis);

  private:

    int16_t AcX, AcY, AcZ;            // accelerometer
    int16_t GyX, GyY, GyZ;            // gyroscope
    int16_t Tmp;                      // temperature

    float Roll, Pitch;
    float fXg, fYg, fZg;

    const float alpha = 0.5;

    int address;
    int sclock;

};

