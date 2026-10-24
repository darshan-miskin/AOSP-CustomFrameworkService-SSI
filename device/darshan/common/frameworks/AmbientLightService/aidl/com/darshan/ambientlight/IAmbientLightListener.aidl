package com.darshan.ambientlight;

/** @hide */
oneway interface IAmbientLightListener {
    void onLuxChanged(int lux);
}