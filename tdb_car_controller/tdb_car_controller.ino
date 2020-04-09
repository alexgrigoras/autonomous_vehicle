
/*=================================== Informations ===================================*
 *                                                                                    *
       The Dream Builders RC Car program for Continental's Electromobility Contest
       Main Program
       Made by The Dream Builders
 *                                                                                    *
  ====================================================================================*/

/*====================================== Header ======================================*/

#include "TDB.h"

/*====================================== Objects =====================================*/

// Ultrasonic
Ultrasonic ultravector[] = {
  Ultrasonic (PING_LEFT, MAX_PING),
  Ultrasonic (PING_FRONT, MAX_PING),
  Ultrasonic (PING_RIGHT, MAX_PING)
};

// Servo
Servo servomotor;               // servo object to control a servomotor
Servo esc;                      // servo object to control a brushless motor with an esc

/*==================================== Variables =====================================*/

// Receiving data over BT
char c;                         // character read
String readString = "";         // string for characters
uint8_t readStr[5];             // store the data to variables
uint8_t str_index = 0;          // index for readStr values

// Sending data over BT
String dataBatt = "";
String dataSpd = "";
String dataDist = "";
String dataWheel = "";
String dataDir = "";
uint8_t dataDistlen;
uint8_t dataWheellen;
String dataErrors = "";
char separator = '|';
char zero = '0';
String sendData = "";
String receiveData = "";

// Infrared
unsigned long prevmillis;       // To store time
unsigned long duration;         // To store time difference
float rpm = 0;
float rps = 0;
boolean currentstate1;          // Current state of IR input scan on sensor 1
boolean currentstate2;          // Current state of IR input scan on sensor 2
boolean prevstate1;             // State of IR sensor in previous scan
boolean prevstate2;             // State of IR sensor in previous scan
int8_t direction = -1;
float carspeed;
unsigned int distanta = 0;

// Accelerometer + Gyroscope
MPU_6050 mpu(0x68, CLOCK_NORMAL);
int16_t AcX, AcY, AcZ;          // accelerometer
int16_t Tmp;                    // temperature
int accel_compensation;

// Ultrasonic sensors values
uint8_t sonar_val[3];
uint8_t leftval;
uint8_t frontval;
uint8_t rightval;

// BT serial
// declaration of software serial port object serial_tc6 which uses timer/counter channel TC6
serial_tc6_declaration(RX_BUF_LENGTH, TX_BUF_LENGTH);
auto& BT = serial_tc6;          // serial_tc6_t& serial_obj=serial_tc6;
boolean BTconnection = false;

// Set de tensiune de referință;
float refVcc = 3.3;
int percentage = 0;
float voltage = 0;

// Timers for sending BT data at 100 ms
Timer timerBT, timerPC, timerBT2;
Timer BTconn;

// PID
double SetpointL, InputL, OutputL;        // Define Variables we'll be connecting to
double SetpointR, InputR, OutputR;
//double Kp = 0.8, Ki = 0.06, Kd = 0;
double Kp = 0.7, Ki = 0.06, Kd = 0;       // Specify the links and initial tuning parameters
PID PID_Left (&InputL, &OutputL, &SetpointL, Kp, Ki, Kd, DIRECT);
PID PID_Right (&InputR, &OutputR, &SetpointR, Kp, Ki, Kd, DIRECT);
int Output;                               // Combined output
int escOut;                               // Output for esc

//Smoothing
float filterValSpd = 0.5;         // this determines smoothness  - .0001 is max  1 is off (no smoothing)
float filterValGyro = 0.8;
int smoothedSpd;                  // this holds the last loop value just use a unique variable for every
int smoothedGyroX;

// Median Filter
//Median <int, 1, 0> USmedVal[3];   // Ultrasonic sensors
Median <int, 5, 0> SpeedMedVal;   // Speed

// Average Filter
const int numReadings = 20;
float readings[numReadings];      // the readings from the analog input
int readIndex = 0;                // the index of the current reading
float total = 0;                  // the running total
int average = 0;                  // the average

// Metal detector
boolean metaldetected = false;
uint8_t linestopped = 0;
boolean frontstop = false;
boolean readline = false;

// Error reporting variables
boolean gyroError = false;
boolean ultrasError[3];
boolean voltageError = false;
boolean speedError = false;
int errors;

// current sensor
double currentval;
double miliamps;
double amps;

#ifdef _DEBUG_
// Car position on track
char w[] = "--------------------------------------------------"; //50
uint8_t vi = 0;
#endif

/*====================================== SETUP =======================================*/

void setup() {

  // BT serial communnication initialization
  BT.begin(
    RX_PIN,
    TX_PIN,
    SOFT_UART_BIT_RATE,
    soft_uart::data_bit_codes::EIGHT_BITS,
    soft_uart::parity_codes::NO_PARITY,
    soft_uart::stop_bit_codes::ONE_STOP_BIT
  );

  // PC serial commmunication initialization
#ifdef _DEBUG_
  PC.begin(115200);
#endif

  // I2C accelerometer + Gyroscope
  mpu.initialize();

  // Initialize readStr values to 0
  for (int i = 0; i < 5; i++) {
    readStr[i] = 0;
  }
  for (int thisReading = 0; thisReading < numReadings; thisReading++) {
    readings[thisReading] = 0;
  }
  for (int i = 0; i < 3; i++) {
    ultrasError[i] = false;
  }

  // Initialization of the sensor pin as an input for reading speed
  pinMode(IRSENSOR1, INPUT);
  pinMode(IRSENSOR2, INPUT);
  digitalWrite(IRSENSOR1, HIGH);   // turn on the pullup
  digitalWrite(IRSENSOR2, HIGH);   // turn on the pullup

  pinMode(METALDETECTOR, INPUT);
  pinMode(SENSORCURRENT, INPUT);
  pinMode(SENSORFAULT, INPUT);

  // Infrared Speed Sensor
  prevmillis = 0;
  prevstate1 = LOW;
  prevstate2 = LOW;

  // Servomotor + esc pin allocation
  servomotor.attach(4);
  esc.attach(5);
  servomotor.write(90);            // Set servo pozition to middle
  esc.write(90);                   // Set esc pozition to neutral

  // Scheduler tasks
  //Scheduler.startLoop(ReceiveBTData);
  Scheduler.startLoop(ReadSpeed);
  Scheduler.startLoop(ReadAccelerometer);
  Scheduler.startLoop(ReadVoltage);
  Scheduler.startLoop(ReadCurrent);
  Scheduler.startLoop(ControlMode);

  // Timers fot sending data at INTERVAL seconds
  timerBT2.every(10, ReceiveBTData);
  timerBT.every(INTERVAL, SendBTData);
  //BTconn.every(1000, bt_check_timer);

  // Voltage Sensor first average value
  average = analogRead(SENSORVOLTAGE);

  // PID
  SetpointL = 25;                           //initialize the variables we're linked to
  SetpointR = 25;
  //turn the PID on
  PID_Left.SetMode(AUTOMATIC);
  PID_Left.SetOutputLimits(-120, 120);
  PID_Left.SetSampleTime(20);
  PID_Right.SetMode(AUTOMATIC);
  PID_Right.SetOutputLimits(-120, 120);
  PID_Right.SetSampleTime(20);
}

/*======================================= LOOP =======================================*/
// Task no.0 = default loop

void loop() {

  // Delay for other tasks to execute
  delay(30);

  // Update BT transfer timer
  timerBT.update();
  timerBT2.update();
  //BTconn.update();

  // Serial debug
#ifdef _DEBUG_
  SendPCData();
#endif

  // Ultrasonic sensor read
  if (readStr[0] == 1) {
    ReadUltrasonic();
  }

  //ReadCurrent();

}

/*==================================== BT Check ======================================*/
// Check if BT communication is available

void bt_check_timer() {
  if (BT.available() > 0) {
    BTconnection = true;
  }
  else {
    BTconnection = false;
  }
}

/*================================= Receive BT Data ==================================*/
// Task no.1 = Read BT data

void ReceiveBTData()
{
  while (BT.available()) {
    delay(3);  //small delay to allow input buffer to fill
    if ((c = BT.read()) >= 0) {
      BTconnection = true;
#ifdef _DEBUG_
      if (BT.data_lost()) {
        Serial.println("[DATA_LOST]");
        BTconnection = false;
        break;
      }
#endif
      if (isDigit(c))
        readString += c;
      else if (c == '|') {
        readStr[str_index] = readString.toInt();
        readString = ""; //clears variable for new input
        str_index++;
      }
      else if (c == '*') {
        readStr[4] = readString.toInt();
        str_index = 0;
        //break;
      }
    }
#ifdef _DEBUG_
    else if (BT.bad_status())
    {
      if (BT.bad_start_bit()) Serial.println("[BAD_START_BIT]");
      if (BT.bad_parity()) Serial.println("[BAD_PARITY]");
      if (BT.bad_stop_bit()) Serial.println("[BAD_STOP_BIT]");
      BTconnection = false;
    }
#endif
    else {
      BTconnection = false;
    }
  }
  if (readString.length() > 0) {
    readString = ""; //clears variable for new input
  }
}

/*=============================== Speed measurement ==================================*/

// Task no.2 = Get speed value
void ReadSpeed() {
  // RPM Measurement
  currentstate1 = digitalRead(IRSENSOR1);     // Read IR sensor state
  currentstate2 = digitalRead(IRSENSOR2);     // Read IR sensor state

  if ( prevstate1 != currentstate1)           // If there is change in input
  {
    if ( currentstate1 == HIGH )              // If input only changes from LOW to HIGH
    {
      duration = ( micros() - prevmillis );   // Time difference between revolution in microsecond
      prevmillis = micros();                  // store time for nect revolution calculation

#ifdef _SPEED_KMH_
      rpm = 30000000 / duration;              // rpm = ((1/ time millis)*1000*1000*60) / 2;
      carspeed = 3.15159 * (WHEEL_DIAMETER / 100000) * ( rpm * 60 ); // speed in km/h
      if (carspeed >= 0 && carspeed <= 99) {
        smoothedSpd =  smooth(carspeed, filterValSpd, smoothedSpd);
        SpeedMedVal.addValue(smoothedSpd);
      }
#endif

#ifdef _SPEED_MS_
      rps = 500000 / duration;                // rps = (1/ time millis)*1000*1000 / 2;
      carspeed = 3.14159 * (WHEEL_DIAMETER / 100) * rps; // speed in m/s
      if (carspeed >= 0 && carspeed <= 99) {
        smoothedSpd =  smooth(carspeed, filterValSpd, smoothedSpd);
        SpeedMedVal.addValue(smoothedSpd);
      }
#endif
    }

    // Direction
    if ( (prevstate1 == LOW && prevstate2 == LOW && currentstate1 == HIGH && currentstate2 == LOW) ||
         (prevstate1 == HIGH && prevstate2 == LOW && currentstate1 == HIGH && currentstate2 == HIGH) ||
         (prevstate1 == HIGH && prevstate2 == HIGH && currentstate1 == LOW && currentstate2 == HIGH) ||
         (prevstate1 == LOW && prevstate2 == HIGH && currentstate1 == LOW && currentstate2 == LOW) ) {
      direction = 1;
      if (distanta > 0) {
        distanta -= 5;
      }
    }
    else if ( (prevstate1 == LOW && prevstate2 == LOW && currentstate1 == LOW && currentstate2 == HIGH) ||
              (prevstate1 == LOW && prevstate2 == HIGH && currentstate1 == HIGH && currentstate2 == HIGH) ||
              (prevstate1 == HIGH && prevstate2 == HIGH && currentstate1 == HIGH && currentstate2 == LOW) ||
              (prevstate1 == HIGH && prevstate2 == LOW && currentstate1 == LOW && currentstate2 == LOW) ) {
      direction = 0;
      distanta += 5;
      if (distanta >= 9999) {
        distanta = 9999;
      }
    }
  }

  // Check error of speed
  if ((esc.readMicroseconds() > 1520 || esc.readMicroseconds() < 1480) && (carspeed == 0)) {
    speedError = true;
  }
  else {
    speedError = false;
  }

  prevstate1 = currentstate1;               // store this scan (prev scan) data for next scan
  prevstate2 = currentstate2;

  yield();
}

/*============================== Voltage measurement =================================*/
// Task no.3 = Get voltage value

void ReadVoltage()
{
  analogReadResolution(10);
  total = total - readings[readIndex];

  readings[readIndex] = analogRead(SENSORVOLTAGE);

  total = total + readings[readIndex];

  readIndex = readIndex + 1;

  if (readIndex >= numReadings) {
    readIndex = 0;
  }

  average = total / numReadings;
  voltage = average * (refVcc / 1024) * 2.45;

  if (voltage > 6) {
    percentage = mapFloat(voltage, 6.2, 8, 5, 99);
    voltageError = false;
  }
  else {
    percentage = 0;
    voltageError = true;
  }

  yield();
}

/*============================== Ultrasonic distance =================================*/
// Task no.4 = Get ultrasonic value

void ReadUltrasonic()
{
  if (readStr[0] == 1) {
    for (int i = 0; i < 3; i++)
    {
      if (ultravector[i].range_cm(sonar_val[i])) {      // get ultrasonic value
        ultrasError[i] = false;
      }
      else {
        ultrasError[i] = true;
      }
      delayMicroseconds(200);
    }
  }
  yield();
}

/*=========================== Accelerometer measurement ==============================*/
// Task no.5 = Get accelerometer value

void ReadAccelerometer()
{
  if (readStr[0] == 1) {
    if (mpu.refresh()) {                                // get new values from mpu
      AcX = mpu.getAccel('p');                          // get accel on x axis
      smoothedGyroX =  smooth(AcX, filterValGyro, smoothedGyroX);
      //Tmp = mpu.getTemp();                              // get temperature
      gyroError = false;
    }
    else {
      gyroError = true;
    }
  }
  else {
    gyroError = false;
  }
  yield();
}

/*=========================== Accelerometer measurement =============================*/
// Task no.6 = Get current value

void ReadCurrent()
{
  analogReadResolution(10);
  
  currentval = analogRead(SENSORVOLTAGE);   
    
  // convert to milli amps
  miliamps = (((long)currentval * 5000 / 1024) - 500 ) * 1000 / 133;
  amps = (float) miliamps / 1000;

  yield();
}

/*================================== Send BT Data ====================================*/
// Task no.7 = Send BT data

void SendBTData()
{
  if (BT.available() > 0) {
    // battery
    if (!voltageError) {
      dataBatt = percentage;
      if (dataBatt.length() == 1) {
        dataBatt = zero + dataBatt;
      }
    } else {
      dataBatt = "Er";
    }
    // speed
    if (smoothedSpd != 0) {
      dataSpd = (int)SpeedMedVal.getMedian();
    }
    else {
      dataSpd = "00";
    }
    if (dataSpd.length() == 1) {
      dataSpd = zero + dataSpd;
    }
    // distance
    dataDist = distanta;
    dataDistlen = dataDist.length();
    for (int i = 4; i > dataDistlen; i--) {
      dataDist = zero + dataDist;
    }
    // wheels position
    dataWheel = servomotor.read();
    dataWheellen = dataWheel.length();
    for (int i = 3; i > dataWheellen; i--) {
      dataWheel = zero + dataWheel;
    }
    // direction
    if (direction == 1) {
      dataDir = "BW";
    } else if (direction == 0) {
      dataDir = "FW";
    } else {
      dataDir = "ST";
    }

    // error package
    errors = 8 * voltageError + 4 * speedError + 2 * (ultrasError[0] | ultrasError[1] | ultrasError[2]) + gyroError;
    dataErrors = errors;
    if (dataErrors.length() == 1) {
      dataErrors = zero + dataErrors;
    }

    // data package
    sendData = '*' + dataBatt + dataSpd + dataDist + dataWheel + dataDir + linestopped + dataErrors + '*';

    // send data
    BT.print(sendData);
  }

  // reset speed + direction
  carspeed = 0;
  smoothedSpd = 0;
  direction = -1;

  //yield();
}

/*============================== PC Debug (send data) ================================*/
// Task no.8 = PC debug (send data)

#ifdef _DEBUG_
void SendPCData()
{
  // Autonomous mode: metal, accelerometer, PID, ultrasonic
  if (BTconnection) {
    if (readStr[0] == 1) {
      /*
      vi = map(Output, -60, 60, 0, 49);
      w[vi] = '|';
      PC.print(w);
      w[vi] = '-';
      */
      if (!gyroError) {
        PC.print("  AcX: ");      PC.print(smoothedGyroX);
      }
      else {
        PC.print("  Gyro Error! ");
      }
      PC.print("   Metal: ");      PC.print(digitalRead(METALDETECTOR));
      PC.print("   Line: ");      PC.print(linestopped);
      PC.print("   PID out: ");    PC.print(Output);
      PC.print("   Ultras: ");     PC.print(sonar_val[0]);
      PC.print(" - ");             PC.print(sonar_val[1]);
      PC.print(" - ");             PC.print(sonar_val[2]);
      PC.print(" | ");
    }
    
    /*
    PC.print("  US Err: ");       PC.print(ultrasError[0] | ultrasError[1] | ultrasError[2]);
    PC.print("  V Err: ");        PC.print(voltageError);
    PC.print("  Spd Err: ");      PC.print(speedError);
    PC.print(" | ");
    */
    // BT data received
    for (int i = 0; i < 5; i++) {
      PC.print(readStr[i]);        PC.print(" ");
    }
    PC.print(servomotor.read());
    PC.println();
  }
  else {/*
    currentval = analogRead(SENSORVOLTAGE);   
    
    // convert to milli amps
    miliamps = (((long)currentval * 5000 / 1024) - 500 ) * 1000 / 133;
    amps = (float) miliamps / 1000;
    
    PC.print("   MiliAmps: ");    PC.print(miliamps);
    PC.print("   Amps: ");        PC.print(amps);
    PC.print("   Fault: ");       PC.print(digitalRead(SENSORFAULT));
    PC.println(" | ");*/
    PC.println(" NO Communication! ");
  }

}
#endif

/*================================== Control Mode ====================================*/
// Task no.9 = Driving mode: manual / autonomous / test

void ControlMode()
{
  if (BTconnection) {
    // Test Mode
    if (readStr[0] == 2) {

      // servomotor
      if (readStr[1] >= 39 && readStr[1] <= 41) {
        // middle
        idle(servomotor);
      } else if (((readStr[1] >= 1 && readStr[1] < 39) || (readStr[1] > 41 && readStr[1] <= 79))) {
        // left / right
        wheels(readStr[1] + 48);
      }

      // esc
      switch (readStr[3]) {
        case 0: // reverse
          reverse(readStr[2] * 2);
          break;
        case 1: // idle
          idle(esc);
          break;
        case 2: // forward
          accelerate(readStr[2]);
          break;
        default:
          idle(esc);
          break;
      }
      yield();
    }

    // Manual mode
    else if (readStr[0] == 0) {

      // servomotor
      if (readStr[1] >= 39 && readStr[1] <= 41) {
        // middle
        idle(servomotor);
      } else if (((readStr[1] >= 1 && readStr[1] < 39) || (readStr[1] > 41 && readStr[1] <= 79))) {
        // left / right
        wheels(readStr[1] + 50);
      }

      // esc
      switch (readStr[3]) {
        case 0: // reverse
          for (int i = 0; i < readStr[2] - 5; i++) {
            reverse(readStr[2] * 2);
          }
          break;
        case 1: // idle
          idle(esc);
          break;
        case 2: // forward
          for (int i = 0; i < readStr[2] - 5; i++) {
            accelerate(i);
          }
          break;
        default:
          idle(esc);
          break;
      }
      yield();
    }

    // Autonomous mode
    else if (readStr[0] == 1) {

      readline = digitalRead(METALDETECTOR);

      // start car
      if (readStr[4] == 1) {
        /*
        if(readline == 1 && distanta > 100){
          linestopped = 1;
        }*/

        // get ultrasonic values
        leftval = sonar_val[0];
        frontval = sonar_val[1];
        rightval = sonar_val[2];

        // input values to PID
        InputR = rightval;
        InputL = leftval;

        // Calculate PID
        PID_Left.Compute();
        PID_Right.Compute();

        // output
        Output = OutputR - OutputL;

        // output limits
        if (Output > 60) {
          Output = 60;
        }
        if (Output < -60) {
          Output = -60;
        }

        // calculate esc value
        escOut = Output;
        escOut = map(Output, -60, 60, -5, 5);
        if (escOut < 0) {
          escOut = escOut * (-1);
        }
        escOut = 5 - escOut;

        // orientation compensation
        if (smoothedGyroX > 10 && smoothedGyroX < 50) {
          accel_compensation = smoothedGyroX*2;
        }
        else if (smoothedGyroX < -10 && smoothedGyroX > - 50) {
          accel_compensation = smoothedGyroX/2;
        }
        else {
          accel_compensation = 0;
        }

        // set servo
        wheels(90 + Output);

        // set esc
        if (frontval > 20) {
          accelerate(10 + escOut + accel_compensation);
        }
        else {
          idle(esc);
        }
      }

      // idle car
      else {
        idle(servomotor);                           // set servo + esc to idle
        idle(esc);
        linestopped = 0;                            // line detection reset

        InputL = 0;                                 // reset pid
        OutputL = 0;
        InputR = 0;
        OutputR = 0;
        Output = 0;
      }
      yield();
    }
  }
  else {
    // no connection => idle car
    idle(servomotor);                               // set servo + esc to idle
    idle(esc);
  }

  yield();
}

/*====================================== END =========================================*/
