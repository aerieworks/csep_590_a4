#include <RFduinoBLE.h>

const int sensorPin = 6;
const unsigned int READ_DELAY = 10;

volatile unsigned int sensorValue = 0;
volatile unsigned long lastReadTime = 0;
volatile unsigned long currentTime = 0;
volatile bool connected = false;

void setup() {
  Serial.begin(2400);
  
  RFduinoBLE.deviceName = "richanna<3mon";
  RFduinoBLE.advertisementInterval = 1000;
  RFduinoBLE.begin();
}

void loop() {
  // Stay in ultra-low power mode.
  //RFduino_ULPDelay(INFINITE);
  if (connected) {
    sensorValue = analogRead(sensorPin);
    Serial.print(millis());
    Serial.print(":\t");
    Serial.println(sensorValue);
    RFduinoBLE.sendInt(sensorValue);
  }
  delay(READ_DELAY);
}

void RFduinoBLE_onAdvertisement(bool start) {
  if (start) {
    Serial.println("Started advertising.");
  } else {
    Serial.println("Stopped advertising.");
  }
}

void RFduinoBLE_onConnect() {
  connected = true;
  //interruptSetup();
  Serial.println("Connected.");
}

void RFduinoBLE_onDisconnect() {
  //cli();
  connected = false;
  Serial.println("Disconnected.");
}
