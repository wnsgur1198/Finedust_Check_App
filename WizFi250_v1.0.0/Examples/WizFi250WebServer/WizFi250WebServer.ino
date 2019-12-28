#include <Arduino.h>
#include <SPI.h>
#include <IPAddress.h>
#include <WizFi250.h>
#include <WizFi250_tcp_server.h>


#define SSID      "Gggh2"
#define KEY     "12345678"
//#define SSID      "SAMSUNG"
//#define KEY     "zxczxc6600@"
#define AUTH      ""
#define LOCAL_PORT    8080

//--미세먼지센서
#include <SoftwareSerial.h>  
SoftwareSerial mySerial(7,6); // Arudino Uno port RX, TX  
#define START_1 0x42  
#define START_2 0x4d  
#define DATA_LENGTH_H        0  
#define DATA_LENGTH_L        1  
#define PM10_ATMOSPHERE_H    12  
#define PM10_ATMOSPHERE_L    13
#define VERSION              26  
#define ERROR_CODE           27  
#define CHECKSUM             29  
byte bytCount1 = 0;  
byte bytCount2 = 0;  
unsigned char chrRecv;  
unsigned char chrData[30];  
int PM10; 

unsigned int GetPM_Data(unsigned char chrSrc[], byte bytHigh, byte bytLow)  
{  
   return (chrSrc[bytHigh] << 8) + chrSrc[bytLow];  
}
//--미세먼지센서

boolean   Wifi_setup = false;
boolean   currentLineIsBlank = false;
boolean   IsSendHtmlResponse = false;
WizFi250  wizfi250;
uint8_t   currentLineIsBlankCnt = 0;

WizFi250_TCP_Server myServer(LOCAL_PORT);

void sendData();

//The setup function is called once at startup of the sketch
void setup()
{
  // Add your initialization code here
  Serial.begin(9600);
  Serial.println("\r\nSerial Init");

  mySerial.begin(9600); 

  wizfi250.begin();
  wizfi250.setDebugPrint(4);
  wizfi250.hw_reset();

  wizfi250.sync();
  wizfi250.setDhcp();
  //  wizfi250.defaultWebServerDown();

  for(int i=0; i<10; i++)   // Try to join 30 times
  {
    if( wizfi250.join(SSID,KEY,AUTH) == RET_OK )
    {
      Wifi_setup = true;
      break;
    }
  }
}

// The loop function is called in an endless loop
void loop()
{
  if( Wifi_setup )
  {
    wizfi250.RcvPacket();
    if( myServer.isListen() != true )
    {
      myServer.listen();
    }
    if( myServer.available())
    {
      if( IsSendHtmlResponse == true )
      {
        IsSendHtmlResponse=false;
        myServer.stop();
      }
      char c = myServer.recv();
      if( c != NULL )
      {
        Serial.print((char)c);
        if(c == '\n' && currentLineIsBlank)
        {
          sendData();
          currentLineIsBlank = false;
          IsSendHtmlResponse = true;
        }
        if(c == '\n')
        {
          currentLineIsBlank = true;
        }
        else if(c != '\r')
          currentLineIsBlank = false;
      }
    }
  }
}

void sendData()
{
  String TxData;
  uint8_t temp_value[10] = {
    0  };

  TxData = "HTTP/1.1 200 OK\r\n";
  TxData += "Content-Type: text/html\r\n";
  TxData += "Connection: close\r\n";
  TxData += "Refresh: 5\r\n";
  TxData += "\r\n";
  //TxData += "<!DOCTYPE HTML>\r\n";
  TxData += "<html>\r\n";

  //--미세먼지센서--
    if (mySerial.available())   {  
       for(int i = 0; i < 32; i++)     {  
           chrRecv = mySerial.read();  
           if (chrRecv == START_2 ) {   
              bytCount1 = 2;  
              break;  
            }  
       }   
      if (bytCount1 == 2)  
      {  
         bytCount1 = 0;  
         for(int i = 0; i < 30; i++){  
            chrData[i] = mySerial.read();  
         }   
  
         if ( (unsigned int) chrData[ERROR_CODE] == 0 ) {  
            PM10  = GetPM_Data(chrData, PM10_ATMOSPHERE_H, PM10_ATMOSPHERE_L);  
            }  
      }   
   }  
  //--미세먼지센서--
  int sensorReading = PM10;
  //TxData += "analog input ";
  //itoa(analogChannel,(char*)temp_value, 10);
  //TxData += (char*)temp_value;
  //TxData += " is ";
  itoa(sensorReading,(char*)temp_value, 10);
  TxData += (char*)temp_value;
  //TxData += (char*)"10";
  //TxData += "<br />\r\n";
  
  TxData += "</html>\r\n";
  //TxData += "\r\n";
  //TxData += "\r\n";

  myServer.send(TxData);
}
