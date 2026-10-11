#pragma once

#include <WString.h>

namespace NvStore {
enum class Key : unsigned int {
    WIFI_SSID,
    WIFI_PSK,
    SERVER_IP,

    count, // sentinel
};

const char *const mapKeyToString[static_cast<unsigned int>(Key::count)] = {
    "wifi_ssid",
    "wifi_psk",
    "server_ip",
};

// Functions to retrieve and store key-values in store.
String get(Key key);
void put(Key key, String value);

// Functions to be called in Arduino framework entry points.
void setup(void);
}
