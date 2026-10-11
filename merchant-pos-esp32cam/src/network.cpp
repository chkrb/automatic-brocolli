#include <WiFi.h>

#include "network.h"
#include "nvstore.h"

static bool forceAccessPointMode = true;

/*
 * Public: related to Wi-Fi access point (AP) mode.
 */

bool Network::accessPointEnabled(void) {
    return WiFi.getMode() & WIFI_MODE_AP;
}

unsigned int Network::accessPointNumStations(void) {
    return WiFi.softAPgetStationNum();
}

String Network::accessPointNetworkSsid(void) {
    static String ssid;

    if (ssid.isEmpty()) {
        String macAddressCompact = WiFi.softAPmacAddress().substring(9);
        macAddressCompact.remove(5, 1);
        macAddressCompact.remove(2, 1);

        ssid += "PQ-POS-";
        ssid += macAddressCompact;
    }

    return ssid;
}

String Network::accessPointNetworkPsk(void) {
    static String psk;

    if (psk.isEmpty()) {
        for (int i = 0; i < 63; i++) {
            long rng = random() % (10 + 26 + 26);

            if (rng < 10) {
                psk += static_cast<char>('0' + rng);
            } else if (rng < 10 + 26) {
                psk += static_cast<char>('A' + rng - 10);
            } else {
                psk += static_cast<char>('a' + rng - 10 - 26);
            }
        }
    }

    return psk;
}

IPAddress Network::accessPointNetworkAddress(void) {
    return WiFi.softAPIP();
}

/*
 * Public: related to Wi-Fi station (STA) mode.
 */

bool Network::stationConnected(void) {
    return WiFi.isConnected() && !accessPointEnabled();
}

void Network::stationNetworkReconfigure(void) {
    String ssid = NvStore::get(NvStore::Key::WIFI_SSID);
    String psk = NvStore::get(NvStore::Key::WIFI_PSK);

    if (!ssid.isEmpty()) {
        WiFi.begin(ssid, psk);
        forceAccessPointMode = false;
    }
}

/*
 * Public: to be called in Arduino framework entry points.
 *
 * Set up or connect to the internet. The merchant server is also to be a Wi-Fi
 * access point, and the POSes connect to it as clients. The POS require setup
 * in order to get the Wi-Fi credentials and details.
 *
 * The SSID and PSK of the merchant server network is stored in the
 * non-volatile storage. However, with a new device this needs to be set up.
 * The device will act as a soft access point, to which the administrator will
 * connect and configure settings through a web server.
 */

#define SETUP_PIN 0

void Network::setup(void) {
    pinMode(SETUP_PIN, INPUT_PULLUP);
    stationNetworkReconfigure();
}

void Network::loop(unsigned long dt) {
    static bool connectedStatus = false;
    static bool lastSetupPinValue = true;
    static int dtAccum = 0;

    bool setupPinValue = digitalRead(SETUP_PIN);

    // Only allow one loop call per second. However, allow for changes in
    // hardware reporting.
    dtAccum += dt;
    if (dtAccum < 1000 && setupPinValue == lastSetupPinValue) {
        return;
    }

    dtAccum %= 1000;
    lastSetupPinValue = setupPinValue;

    // For factory devices, the SSID and PSK of the network are not set.
    // Force direct to the access point mode to continue with the setup.
    if (forceAccessPointMode) {
        setupPinValue = false;
    }

    if (!setupPinValue && !(WiFi.getMode() & WIFI_MODE_AP)) {
        if (WiFi.getMode() & WIFI_MODE_STA) {
            WiFi.disconnect(true);
        }

        WiFi.enableAP(true);
        WiFi.softAP(accessPointNetworkSsid(), accessPointNetworkPsk());

        Serial.println("Connect to the Wi-Fi network:");
        Serial.print("* SSID: ");
        Serial.println(WiFi.softAPSSID());
        Serial.print("* Device IP: ");
        Serial.println(WiFi.softAPIP());
        Serial.println("Configure the POS Wi-Fi network using the web interface.");
        return;
    }

    if (setupPinValue && !(WiFi.getMode() & WIFI_MODE_STA)) {
        if (WiFi.getMode() & WIFI_MODE_AP) {
            WiFi.softAPdisconnect(true);
        }

        WiFi.enableSTA(true);
        WiFi.begin();
        connectedStatus = false;
    }

    if ((WiFi.getMode() & WIFI_MODE_STA) && WiFi.isConnected() && !connectedStatus) {
        Serial.println("Connected to the Wi-Fi network:");
        Serial.print("* SSID: ");
        Serial.println(WiFi.SSID());
        Serial.print("* Device IP: ");
        Serial.println(WiFi.localIP());
        connectedStatus = true;
    }
}
