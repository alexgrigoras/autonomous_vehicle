
/*=================================== Informations ===================================*
 *                                                                                    *
       The Dream Builders RC Car program for Continental's Electromobility Contest
       Header
       Made by The Dream Builders
       
       Libraries used: Scheduler, Servo, soft_uart, Timer, Wire
 *                                                                                    *
  ====================================================================================*/

/*======================================== HEADER =======================================*/

#ifndef TDB_h
#define TDB_h

/*====================================== Libraries ======================================*/
#include <Scheduler.h>
#include <Servo.h>
#include <Timer.h>
#include <soft_uart.h>
using namespace arduino_due;

#include "Median.h"
#include "PID.h"
#include "Ping.h"
#include "MPU_6050.h"

/*================================= Define / variables ==================================*/

// DEBUG PC
#define _DEBUG_

#define PC Serial

// Ultrasonic
#define PING_LEFT       22            // Arduino pin tied to echo pin on the ultrasonic sensor left
#define PING_FRONT      26            // Arduino pin tied to echo pin on the ultrasonic sensor front
#define PING_RIGHT      30            // Arduino pin tied to echo pin on the ultrasonic sensor right
#define MAX_PING        6000          // Maximum distance we want to ping ~= 100 cm 

// Software serial - Bluetooth
#define RX_PIN 10                     // software serial port's reception pin
#define TX_PIN 11                     // software serial port's transmision pin
#define SOFT_UART_BIT_RATE 57600      // baudrate
#define RX_BUF_LENGTH 256             // software serial port's reception buffer length
#define TX_BUF_LENGTH 256             // software serial port's transmision buffer length

// Speed sennsors (infrared)
#define IRSENSOR1 7
#define IRSENSOR2 8

// Metal detector
#define METALDETECTOR 12

// Wheel Diameter
#define WHEEL_DIAMETER 10.0           // cm

// Speed Km/h or m/s
#define _SPEED_KMH_
//#define _SPEED_MS_

// Voltage sennsor (ADC)
#define SENSORVOLTAGE A0

// Current sennsor (ADC)
#define SENSORCURRENT A2
#define SENSORFAULT A3

// Interval for sending data
#define INTERVAL 100

// Servo
extern Servo servomotor;              // servo object to control a servomotor
extern Servo esc;                     // servo object to control a motor (bldc) esc

/*====================================== Functions ======================================*/

void wheels(int angle);

void accelerate(int speed_val);

void brake(int speed_val);

void reverse(int speed_val);

void idle(Servo motor);

float mapFloat(float x, float in_min, float in_max, float out_min, float out_max);

int smooth(int data, float filterVal, float smoothedVal);

#endif

/*========================================= END =========================================*/

