#include <cstdio>

#include <ArduinoHttpServer.h>
#include <Esp.h>
#include <JSON.h>
#include <WiFi.h>

#include "nvstore.h"
#include "webconfig.h"

#define BODY_SZ 512

extern const char indexHtml[] asm("_binary_src_webconfig_index_html_start");

static WiFiServer server;

static void responseIndex(ArduinoHttpServer::StreamHttpRequest<BODY_SZ> &httpRequest) {
    if (httpRequest.getMethod() != ArduinoHttpServer::Method::Get) {
        // TODO: reply with error
        return;
    }

    ArduinoHttpServer::StreamHttpReply httpReply(httpRequest.getStream(), "text/html");

    String wifiSsidString = NvStore::get(NvStore::Key::WIFI_SSID);
    String wifiPskString = NvStore::get(NvStore::Key::WIFI_PSK);
    String serverIpString = NvStore::get(NvStore::Key::SERVER_IP);
    const char *wifiSsid = wifiSsidString.c_str();
    const char *wifiPsk = wifiPskString.c_str();
    const char *serverIp = serverIpString.c_str();

    const size_t length = snprintf(nullptr, 0, indexHtml, wifiSsid, wifiPsk, serverIp);
    char *content = new char[length];

    snprintf(content, length, indexHtml, wifiSsid, wifiPsk, serverIp);
    httpReply.send(content);

    delete[] content;
}

static void responseReset(ArduinoHttpServer::StreamHttpRequest<BODY_SZ> &httpRequest) {
    if (httpRequest.getMethod() != ArduinoHttpServer::Method::Post) {
        // TODO: reply with error
        return;
    }

    if (httpRequest.getContentType() == "application/json") {
        JSON payload(httpRequest.getBody());
        if (!payload.isValid()) {
            // TODO: reply with error
            return;
        }

        const size_t bufLength = 64;
        char *buf = new char[bufLength];

        for (int i = 0; i < static_cast<int>(NvStore::Key::count); i++) {
            auto jsonKey = F(NvStore::mapKeyToString[i]);

            if (payload.hasKey(jsonKey)) {
                payload.getString(jsonKey, buf, bufLength);

                NvStore::Key key = static_cast<NvStore::Key>(i);
                NvStore::put(key, buf);
            }
        }

        delete[] buf;
    }

    ArduinoHttpServer::StreamHttpReply httpReply(httpRequest.getStream(), "application/json");
    httpReply.send("{}");

    ESP.restart();
}

/*
 * Public: to be called in Arduino framework entry points.
 *
 * This sets up an HTTP server which is used to configure the device settings
 * of the POS terminal.
 */

void WebConfig::loop(void) {
    server.begin();

    WiFiClient client = server.available();
    if (!client) {
        client.flush();
        client.stop();
        return;
    }

    ArduinoHttpServer::StreamHttpRequest<BODY_SZ> httpRequest(client);
    if (!httpRequest.readRequest()) {
        ArduinoHttpServer::StreamHttpErrorReply httpReply(client, "text/html");
        String errorMessage(httpRequest.getError().cStr());

        httpReply.send(errorMessage);

        client.stop();
        return;
    }

    if (httpRequest.getResource().toString() == "/") {
        responseIndex(httpRequest);
    } else if (httpRequest.getResource().toString() == "/reset") {
        responseReset(httpRequest);
    } else {
        ArduinoHttpServer::StreamHttpErrorReply httpReply(client, "text/html", "404");
        httpReply.send("Resource not found.");
    }

    client.stop();
}
