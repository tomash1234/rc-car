#define USE_WIFI false

#include <WiFiUdp.h>
#include <ESP8266WiFi.h>


// Set those variables only if USE_WIFI = true
#define LOCAL_PORT 5101
#define PACKET_SIZE 3

#define WIFI_SSID "TomasProjects"
#define WIFI_PASSWORD "TomasProjects"
//

#define PIN_RELE_BACKWARD D5
#define PIN_RELE_FORWARD D6
#define PIN_RELE_LEFT D7
#define PIN_RELE_RIGHT D8


WiFiUDP Udp; 

void connect_to_wifi(const char* ssid, const char* password){
  WiFi.mode(WIFI_STA);
  WiFi.disconnect();
  delay(100);
  
  WiFi.begin(ssid, password); 
  int i = 0;
  while (WiFi.status() != WL_CONNECTED) {
    delay(1000);
    Serial.print(++i); Serial.print(' ');
    if(i > 10){
      Serial.println("Failed! No connection");  
      for(;;){}
      break;
    }
  }
  Serial.println("Connection established!");  
  Serial.print("IP address:\t");
  Serial.println(WiFi.localIP());
}

void setup() {
  Serial.begin(9600);
  pinMode(PIN_RELE_BACKWARD, OUTPUT);
  pinMode(PIN_RELE_FORWARD, OUTPUT);
  pinMode(PIN_RELE_LEFT, OUTPUT);
  pinMode(PIN_RELE_RIGHT, OUTPUT);


  if(USE_WIFI){
    connect_to_wifi(WIFI_SSID, WIFI_PASSWORD);
    Udp.begin(LOCAL_PORT);
  }else{
    Serial.println("Board started");
  }
}

void turn_left(){
  digitalWrite(PIN_RELE_LEFT, HIGH);
  digitalWrite(PIN_RELE_RIGHT, LOW);
}

void turn_right(){
  digitalWrite(PIN_RELE_LEFT, LOW);
  digitalWrite(PIN_RELE_RIGHT, HIGH);
}

void turn_streight(){
  digitalWrite(PIN_RELE_LEFT, LOW);
  digitalWrite(PIN_RELE_RIGHT, LOW);
}


void move_forward(){
  
  digitalWrite(PIN_RELE_BACKWARD, LOW);
  digitalWrite(PIN_RELE_FORWARD, HIGH);
}

void move_backward(){
  
  digitalWrite(PIN_RELE_BACKWARD, HIGH);
  digitalWrite(PIN_RELE_FORWARD, LOW);
}

void move_stop(){
  
  digitalWrite(PIN_RELE_BACKWARD, LOW);
  digitalWrite(PIN_RELE_FORWARD, LOW);
}


void read_packets(){
  if(Udp.parsePacket()){ 
    uint8_t received[PACKET_SIZE];
    int len = Udp.read(received, PACKET_SIZE);
    if(len == PACKET_SIZE && received[0] == 1){
      if(received[1] == 1){
        turn_left();
      } else if(received[1] == 2){
        turn_right();
      } else if(received[1] == 3){
        turn_streight();
      }

      if(received[2] == 1){
        move_forward();
      } else if(received[2] == 2){
        move_backward();
      } else if(received[2] == 3){
        move_stop();
      }
    }
  }
}

void read_serial(){
  while(Serial.available() >= 3){
    byte control = Serial.read();
    byte steering = Serial.read();
    byte motor = Serial.read();

    
    if(steering == 1){
      turn_left();
    } else if(steering == 2){
      turn_right();
    } else if(steering == 3){
      turn_streight();
    }

    if(motor == 1){
      move_forward();
    } else if(motor == 2){
      move_backward();
    } else if(motor == 3){
      move_stop();
    }

    Serial.print("Received: ");
    Serial.print(motor);  
    Serial.print(steering);
  }
}

void loop() {
  if(USE_WIFI){
    read_packets();
  }else{
    read_serial();
  }
}
