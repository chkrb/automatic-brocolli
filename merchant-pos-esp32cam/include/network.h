#pragma once

#include <IPAddress.h>
#include <WString.h>

namespace Network {
// Functions related to Wi-Fi access point (AP) mode.
bool accessPointEnabled(void);
unsigned int accessPointNumStations(void);
String accessPointNetworkSsid(void);
String accessPointNetworkPsk(void);
IPAddress accessPointNetworkAddress(void);

// Functions related to Wi-Fi station (STA) mode.
bool stationConnected(void);
void stationNetworkReconfigure(void);

// Functions to be called in Arduino framework entry points.
void setup(void);
void loop(unsigned long dt);
}
