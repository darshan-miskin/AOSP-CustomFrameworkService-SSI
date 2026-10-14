package com.darshan.ambientlight;

import com.darshan.ambientlight.IAmbientLightListener;

@VintfStability
/** @hide */
interface IAmbientLightService{
    int getLux();
    void setLux(int lux);

    void registerCallback(IAmbientLightListener listener);
    void unregisterCallback(IAmbientLightListener listener);
}
