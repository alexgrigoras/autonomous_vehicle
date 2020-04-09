/*====================================== Header ======================================*/

#include "TDB.h"

/*==================================== Functions =====================================*/

void wheels(int angle)
{
  //if (servomotor.read() != angle) {
    servomotor.write(angle);
  //}
}

void accelerate(int speed_val)
{
  esc.writeMicroseconds(1500 + speed_val * 2);
}

void reverse(int speed_val)
{
  esc.writeMicroseconds(1500 - speed_val * 2);
}

void idle(Servo motor)
{
  if (motor.readMicroseconds() != 1500) {
    motor.writeMicroseconds(1500);
  }
}

// Map float function
float mapFloat(float x, float in_min, float in_max, float out_min, float out_max)
{
  return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
}

// Smooth function
int smooth(int data, float filterVal, float smoothedVal)
{
  if (filterVal > 1) {     // check to make sure param's are within range
    filterVal = .99;
  }
  else if (filterVal <= 0) {
    filterVal = 0;
  }

  smoothedVal = (data * (1 - filterVal)) + (smoothedVal  *  filterVal);

  return (int)smoothedVal;
}

/*======================================== END ========================================*/
