#include <algorithm>
#include <cctype>
#include <cstdio>
#include <cstring>

#include <TFT_eSPI.h>
#include <qrcode.h>

#include "display.h"
#include "network.h"

TFT_eSPI tft;
static Display::Page currentPage = Display::Page::NONE;

#define COLOR_SURFACE tft.color24to16(0xffffff)
#define COLOR_TEXT tft.color24to16(0x000000)
#define COLOR_PRIMARY tft.color24to16(0x000000)
#define COLOR_SECONDARY tft.color24to16(0x000000)
#define COLOR_ACCENT tft.color24to16(0x000000)

typedef struct {
    int x;
    int y;
    unsigned int width;
    unsigned int height;
} WidgetBox;

#define WB_X_PLUS_WIDTH(__wb) ((__wb).x + static_cast<int>((__wb).width))
#define WB_Y_PLUS_HEIGHT(__wb) ((__wb).y + static_cast<int>((__wb).height))

#define WB_2X_PLUS_WIDTH(__wb) (2 * (__wb).x + static_cast<int>((__wb).width))
#define WB_2Y_PLUS_HEIGHT(__wb) (2 * (__wb).y + static_cast<int>((__wb).height))

static void displayClear(const WidgetBox box) {
    tft.fillRect(box.x, box.y, box.width + 1, box.height + 1, COLOR_SURFACE);
}

static void displayLoadingRing(const WidgetBox box, const int step) {
    if (box.width != box.height) {
        Serial.printf("ERR (%s) box.width != box.height\n", __func__);
        return;
    }

    static const int lengthMin = 60;
    static const int lengthMax = 300;

    static int centerOffset = 0;
    static int length = (lengthMax + lengthMin) / 2;
    static bool lengthIncreasing = true;

    const int x = box.x + box.width / 2;
    const int y = box.y + box.width / 2;
    const unsigned int outerR = box.width / 2;
    const unsigned int innerR = outerR * 4 / 5;

    unsigned int angleFrom = (centerOffset - length / 2 + 360) % 360;
    unsigned int angleTo = (centerOffset + length / 2) % 360;

    tft.drawArc(x, y, outerR, innerR, angleTo, angleFrom, COLOR_SURFACE, COLOR_SURFACE);
    tft.drawArc(x, y, outerR, innerR, angleFrom, angleTo, COLOR_ACCENT, COLOR_SURFACE);

    length += step * (lengthIncreasing ? 1 : -1);

    if (length < lengthMin || length > lengthMax) {
        length = std::min(lengthMax, std::max(lengthMin, length));
        lengthIncreasing = !lengthIncreasing;
    }

    centerOffset += step;
    centerOffset %= 360;
}

static void displayQrCode(
    const WidgetBox box,
    const unsigned int version,
    const char *buf,
    const size_t bufLength
) {
    if (box.width != box.height) {
        Serial.printf("ERR (%s) box.width != box.height\n", __func__);
        return;
    }

    QRCode code;
    const size_t encodedDataLength = qrcode_getBufferSize(version);
    unsigned char encodedData[encodedDataLength];

    qrcode_initBytes(&code, encodedData, version, 0, (unsigned char *)buf, bufLength);

    const int codePixelSize = box.width / code.size;
    const int extras = box.width - codePixelSize * code.size;

    for (int i = 0, x = box.x; i < code.size; i++) {
        int width = codePixelSize;

        if (i * (extras + 1) / code.size != (i + 1) * (extras + 1) / code.size) {
            width++;
        }

        for (int j = 0, y = box.y; j < code.size; j++) {
            int height = codePixelSize;

            if (j * (extras + 1) / code.size != (j + 1) * (extras + 1) / code.size) {
                height++;
            }

            if (qrcode_getModule(&code, i, j)) {
                tft.fillRect(x, y, width, height, COLOR_ACCENT);
            }

            y += height;
        }

        x += width;
    }
}

static void displayText(const WidgetBox box, char *string, const GFXfont *font) {
    tft.setTextColor(COLOR_TEXT);
    tft.setFreeFont(font);

    int drawY = box.y;
    char *ptr = string - 1;
    char *block = string;
    size_t blockLength = 0;

    do {
        ptr++;

        if (block && (*ptr == '\0' || isspace(*ptr))) {
            char chr = *ptr;
            int blockTextWidth;

            *ptr = '\0';
            blockTextWidth = tft.textWidth(block);
            *ptr = chr;

            if (blockTextWidth <= box.width) {
                blockLength = ptr - block;
            }

            if (blockTextWidth > box.width || *ptr == '\0') {
                if (blockLength == 0) {
                    Serial.printf("ERR (%s) can't wrap text, increase box.width\n", __func__);
                    Serial.printf(
                        "TIP (%s) to accomodate at least one more line, add %d\n",
                        __func__,
                        blockTextWidth - box.width 
                    );

                    return;
                }

                if (drawY + tft.fontHeight() > WB_Y_PLUS_HEIGHT(box)) {
                    Serial.printf("ERR (%s) can't wrap text, increase box.height\n", __func__);
                    Serial.printf(
                        "TIP (%s) to accomodate at least one more line, add %d\n",
                        __func__,
                        drawY + tft.fontHeight() - WB_Y_PLUS_HEIGHT(box) 
                    );
                    return;
                }

                ptr = block + blockLength;

                *ptr = '\0';
                tft.drawString(block, box.x, drawY);
                *ptr = chr;

                drawY += tft.fontHeight();

                block = NULL;
                blockLength = 0;
            }
        } else if (!block && !isspace(*ptr)) {
            block = ptr;
        }
    } while (*ptr);
}

/* For: Display::Page::LOADING */

static const WidgetBox pageLoadingBox = {
    .x = TFT_WIDTH / 2 - 25,
    .y = TFT_HEIGHT / 2 - 25,
    .width = 50,
    .height = 50,
};

static void showPageLoading(unsigned long dt) {
    displayLoadingRing(pageLoadingBox, dt / 3);
}

static void hidePageLoading(void) {
    displayClear(pageLoadingBox);
}

/* For: Display::Page::CONFIGURE */

static const WidgetBox pageConfigureQrCodeBox = {
    .x = 10,
    .y = 10,
    .width = TFT_WIDTH - 20,
    .height = TFT_WIDTH - 20,
};
static const WidgetBox pageConfigureLoadingRingBox = {
    .x = 10,
    .y = WB_2Y_PLUS_HEIGHT(pageConfigureQrCodeBox),
    .width = 20,
    .height = 20,
};
static const WidgetBox pageConfigureStepTextBox = {
    .x = 10 + WB_X_PLUS_WIDTH(pageConfigureLoadingRingBox),
    .y = WB_2Y_PLUS_HEIGHT(pageConfigureQrCodeBox),
    .width = static_cast<unsigned int>(tft.width()) / 2,
    .height = 30,
};
static const WidgetBox pageConfigureInstructionTextBox = {
    .x = 10,
    .y = 5 + WB_Y_PLUS_HEIGHT(pageConfigureStepTextBox),
    .width = static_cast<unsigned int>(tft.width()) - 20,
    .height = 45,
};

void showPageConfigure(const unsigned long dt) {
    static const String ssid = Network::accessPointNetworkSsid();
    static const String psk = Network::accessPointNetworkPsk();
    static const String ip = Network::accessPointNetworkAddress().toString();

    static unsigned int lastNumStations = Network::accessPointNumStations();
    unsigned int numStations = Network::accessPointNumStations();

    if ((lastNumStations > 0 && numStations == 0) || dt == 0) {
        const size_t qrBufferLength = 1 // NUL
            + 18 // format text
            + 13 // SSID length (PQ-POS-xxxxxx)
            + 63 // PSK length
            ;
        char qrBuffer[qrBufferLength];
        char stepText[] = "Step 1";
        char instructionText[] = "Scan QR to connect to Wi-Fi network";

        snprintf(qrBuffer, qrBufferLength, "WIFI:T:WPA;S:%s;P:%s;;", ssid.c_str(), psk.c_str());
        displayClear(pageConfigureQrCodeBox);
        displayQrCode(pageConfigureQrCodeBox, 5, qrBuffer, strlen(qrBuffer));

        displayClear(pageConfigureStepTextBox);
        displayText(pageConfigureStepTextBox, stepText, &FreeSansBold12pt7b);

        displayClear(pageConfigureInstructionTextBox);
        displayText(pageConfigureInstructionTextBox, instructionText, &FreeSans9pt7b);

        lastNumStations = numStations;
    } else if (lastNumStations == 0 && numStations > 0) {
        const size_t qrBufferLength = 1 // NUL
            + 7 // format text
            + (4 * 3) // 4 octets
            + (3 * 1) // 3 periods
            ;
        char qrBuffer[qrBufferLength];
        char stepText[] = "Step 2";
        char instructionText[] = "Scan QR to configure network settings";

        snprintf(qrBuffer, qrBufferLength, "http://%s", ip.c_str());
        displayClear(pageConfigureQrCodeBox);
        displayQrCode(pageConfigureQrCodeBox, 2, qrBuffer, strlen(qrBuffer));

        displayClear(pageConfigureStepTextBox);
        displayText(pageConfigureStepTextBox, stepText, &FreeSansBold12pt7b);

        displayClear(pageConfigureInstructionTextBox);
        displayText(pageConfigureInstructionTextBox, instructionText, &FreeSans9pt7b);

        lastNumStations = numStations;
    }

    displayLoadingRing(pageConfigureLoadingRingBox, dt / 3);
}

void hidePageConfigure(void) {
    displayClear(pageConfigureQrCodeBox);
    displayClear(pageConfigureLoadingRingBox);
    displayClear(pageConfigureStepTextBox);
    displayClear(pageConfigureInstructionTextBox);
}

/*
 * Public: to modify what's being displayed.
 */

void Display::show(Display::Page page) {
    if (currentPage == page) {
        return;
    }

    switch (currentPage) {
        case Display::Page::NONE:
            break;
        case Display::Page::LOADING:
            hidePageLoading();
            break;
        case Display::Page::CONFIGURE:
            hidePageConfigure();
            break;
    }
    
    currentPage = page;

    switch (currentPage) {
        case Display::Page::NONE:
            break;
        case Display::Page::LOADING:
            showPageLoading(0);
            break;
        case Display::Page::CONFIGURE:
            showPageConfigure(0);
            break;
    }
}

/*
 * Public: to be called in Arduino framework entry points.
 *
 * This display is driven with the TFT_eSPI library, which also doubles up as a
 * primitive drawing library. It also has support for fonts. UI is divided into
 * pages, and only one page is displayed at any single point of time.
 */

void Display::setup(void) {
    tft.init();
    tft.fillScreen(COLOR_SURFACE);
}

void Display::loop(unsigned long dt) {
    switch (currentPage) {
        case Display::Page::NONE:
            break;
        case Display::Page::LOADING:
            showPageLoading(dt);
            break;
        case Display::Page::CONFIGURE:
            showPageConfigure(dt);
            break;
    }
}
