/*======================================== HEADER =======================================*/

#pragma once
#include <Arduino.h>

/*================================== Ultrasonic Class ===================================*/

class Ultrasonic
{
  public:
    Ultrasonic(int pin);
    Ultrasonic(int pin, long TO);
    int range_cm(uint8_t &distance_cm);
    int range_in();

  private:
    int pingPin;
    long Time_out;
    long duration, distance_cm, distance_inc;
};

/*========================================= END =========================================*/
