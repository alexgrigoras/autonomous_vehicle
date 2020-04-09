
#include "MPU_6050.h"

MPU_6050::MPU_6050()
{
  address = 0x6B;
  sclock = CLOCK_FAST;
}

MPU_6050::MPU_6050(unsigned int _address, int _sclock)
{
  address = _address;
  sclock = _sclock;
}

void MPU_6050::initialize()
{
  Wire.begin();
  Wire.beginTransmission(address);
  Wire.setClock(sclock);
  Wire.write(0x6B);                 // PWR_MGMT_1 register
  Wire.write(0);                    // set to zero (wakes up the MPU-6050)
  Wire.endTransmission(true);
  fXg = 0;
  fYg = 0;
  fZg = 0;
}

boolean MPU_6050::refresh()
{
  Wire.beginTransmission(address);
  Wire.write(0x3B);                           // starting with register 0x3B (ACCEL_XOUT_H)
  Wire.endTransmission(false);
  Wire.requestFrom(address, 14, true);        // request a total of 14 registers

  if (Wire.available()) {
    AcX = Wire.read() << 8 | Wire.read();  // 0x3B (ACCEL_XOUT_H) & 0x3C (ACCEL_XOUT_L)
    //AcX = map(AcX, -16000, 17000, -90, 90);

    AcY = Wire.read() << 8 | Wire.read();       // 0x3D (ACCEL_YOUT_H) & 0x3E (ACCEL_YOUT_L)
    //AcY = map(AcY, -16600, 16400, -90, 90);

    AcZ = Wire.read() << 8 | Wire.read();       // 0x3F (ACCEL_ZOUT_H) & 0x40 (ACCEL_ZOUT_L)
    //AcZ = map(AcZ, -17700, 16000, -90, 90);
    
    Tmp = Wire.read() << 8 | Wire.read();       // 0x41 (TEMP_OUT_H) & 0x42 (TEMP_OUT_L)
    Tmp = Tmp / 340.00 + 36.53;                 // Celsius
    //Tmp = Tmp / 340.00 + 36.53) * 9 / 5 + 32; // Fahrenheit

    GyX = Wire.read() << 8 | Wire.read();       // 0x43 (GYRO_XOUT_H) & 0x44 (GYRO_XOUT_L)
    //GyX = map(GyX, -30101, 29900, -100, 101);

    GyY = Wire.read() << 8 | Wire.read();       // 0x45 (GYRO_YOUT_H) & 0x46 (GYRO_YOUT_L)
    //GyY = map(GyY, -30010, 29990, -100, 100);

    GyZ = Wire.read() << 8 | Wire.read();       // 0x47 (GYRO_ZOUT_H) & 0x48 (GYRO_ZOUT_L)
    //GyZ = map(GyZ, -30055, 29945, -100, 101);
    
    //Low Pass Filter
    fXg = AcX * alpha + (fXg * (1.0 - alpha));
    fYg = AcY * alpha + (fYg * (1.0 - alpha));
    fZg = AcZ * alpha + (fZg * (1.0 - alpha));

    //Roll & Pitch Calculations
    //Roll  = (atan2(-fYg, fZg) * 180.0) / M_PI;
    Pitch = (atan2(fXg, sqrt(fYg * fYg + fZg * fZg)) * 180.0) / M_PI;

    return true;
  }
  else {
    return false;
  }
}

int16_t MPU_6050::getAccel(char axis)
{
  if (axis == 'r') {
    return Roll;
  }
  if (axis == 'p') {
    return Pitch;
  }
  if (axis == 'x') {
    return AcX;
  }
  if (axis == 'y') {
    return AcY;
  }
  if (axis == 'z') {
    return AcZ;
  }
}

int16_t MPU_6050::getTemp()
{
  return Tmp;
}

int16_t MPU_6050::getGyro(char axis)
{
  if (axis == 'x') {
    return GyX;
  }
  if (axis == 'y') {
    return GyY;
  }
  if (axis == 'z') {
    return GyZ;
  }
}

