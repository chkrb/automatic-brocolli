#include <Arduino.h>

// #include "camera.h"
#include "display.h"
#include "network.h"
#include "nvstore.h"
#include "webconfig.h"

void setup(void) {
    Serial.begin(115200);

    // Initialize non-volatile storage first, as data inside it may be used in
    // setup calls of other modules.
    NvStore::setup();

    // Camera::setup();
    Display::setup();
    Network::setup();
}

void loop(void) {
    static unsigned long syncDeltaTime = 1000 / 60; // 60 FPS
    static unsigned long currentDeltaTime = 0;

    unsigned long beginTickMillis = millis();
    unsigned long endTickMillis;

    Display::loop(currentDeltaTime);
    Network::loop(currentDeltaTime);

    if (Network::stationConnected()) {
        Display::show(Display::Page::NONE);
        WebConfig::loop();
    } else if (Network::accessPointEnabled()) {
        Display::show(Display::Page::CONFIGURE);
        WebConfig::loop();
    } else {
        Display::show(Display::Page::LOADING);
    }

    endTickMillis = millis();

    if (endTickMillis - beginTickMillis < syncDeltaTime) {
        delay(syncDeltaTime - (endTickMillis - beginTickMillis));
    }

    currentDeltaTime = millis() - beginTickMillis;
}
