#include <RFduinoBLE.h>

const int SENSOR_PIN = 6;
const unsigned int WINDOW_SIZE = 8;
const unsigned int READ_DELAY = 10;

bool connected = false;

unsigned int window[WINDOW_SIZE];
unsigned int windowIndex = 0;
unsigned int windowTotal = 0;

void setup() {
  for (int i = 0; i < WINDOW_SIZE; i++) {
    window[i] = 0;
  }
  
  Serial.begin(2400);
  
  RFduinoBLE.deviceName = "richanna<3mon";
  RFduinoBLE.advertisementInterval = 1000;
  RFduinoBLE.begin();
}

void loop() {
  if (connected) {
    unsigned int sensorValue = analogRead(SENSOR_PIN);
    float demeanedValue = getDemeanedValue(sensorValue);
    RFduinoBLE.sendFloat(demeanedValue);
    Serial.println(demeanedValue);
  }
  
  delay(READ_DELAY);
}

float getDemeanedValue(unsigned int sensorValue) {
  windowTotal += sensorValue - window[windowIndex];
  window[windowIndex] = sensorValue;
  windowIndex = (windowIndex + 1) % WINDOW_SIZE;
  return (float)sensorValue - ((float)windowTotal / (float)WINDOW_SIZE);
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
  Serial.println("Connected.");
}

void RFduinoBLE_onDisconnect() {
  connected = false;
  Serial.println("Disconnected.");
}
