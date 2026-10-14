package com.darshan.ambientlight;

@VintfStability
/** @hide */
oneway interface IAmbientLightListener {
    void onLuxChanged(int lux);
}
