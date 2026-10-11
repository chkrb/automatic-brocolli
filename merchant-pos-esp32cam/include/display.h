#pragma once

#include <WString.h>

namespace Display {
enum class Page {
    NONE,
    LOADING,
    CONFIGURE,
};

// Functions to modify what's being displayed.
void show(Page page);

// Functions to be called in Arduino framework entry points.
void setup(void);
void loop(unsigned long dt);
}
