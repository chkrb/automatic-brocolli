#include <Preferences.h>
#include <WString.h>

#include "nvstore.h"

static Preferences store;

/* Public: Functions to retrieve and store key-values in store. */

String NvStore::get(NvStore::Key key) {
    return store.getString(NvStore::mapKeyToString[static_cast<int>(key)]);
}

void NvStore::put(NvStore::Key key, String value) {
    store.putString(NvStore::mapKeyToString[static_cast<int>(key)], value);
}

/*
 * Public: to be called in Arduino framework entry points.
 */

void NvStore::setup() {
    store.begin("merchant-pos");
}
