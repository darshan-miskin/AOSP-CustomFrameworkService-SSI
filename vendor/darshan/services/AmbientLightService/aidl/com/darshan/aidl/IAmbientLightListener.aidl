package android.os;

/** @hide */
oneway interface IAmbientLightListener {
    void onLuxChanged(int lux);
}