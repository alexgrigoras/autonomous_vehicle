/*====================================== Header ======================================*/

#include "Ping.h"

/*==================================== Functions =====================================*/

Ultrasonic::Ultrasonic(int pin)
{
  pingPin = pin;
  Time_out = 3000;  // 3000 µs = 50cm // 30000 µs = 5 m 
}

Ultrasonic::Ultrasonic(int pin, long TO)
{
  pingPin = pin;
  Time_out = TO;
}

int Ultrasonic::range_cm(uint8_t &distance_cm)
{
  pinMode(pingPin, OUTPUT);
  digitalWrite(pingPin, LOW);
  delayMicroseconds(2);
  digitalWrite(pingPin, HIGH);
  delayMicroseconds(5);
  digitalWrite(pingPin, LOW);
 
  pinMode(pingPin, INPUT);
  duration = pulseIn(pingPin, HIGH, Time_out);
  
  if ( duration == 0 ) {
    duration = Time_out;
    distance_cm = duration / 58 ;
    return 0;
  }
  else {
    distance_cm = duration / 58 ;
    return 1;
  }
}

int Ultrasonic::range_in()
{
  pinMode(pingPin, OUTPUT);
  digitalWrite(pingPin, LOW);
  delayMicroseconds(2);
  digitalWrite(pingPin, HIGH);
  delayMicroseconds(5);
  digitalWrite(pingPin, LOW);
 
  pinMode(pingPin, INPUT);
  duration = pulseIn(pingPin, HIGH, Time_out);
  
  if ( duration == 0 ) {
    duration = Time_out; 
  }
  distance_inc = duration / 128;
  
  return distance_inc; 
}

/*======================================== END =======================================*/

