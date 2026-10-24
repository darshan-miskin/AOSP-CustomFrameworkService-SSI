/**
  Other SystemServer variables and functions
*/
  
private void startOtherServices(@NonNull TimingsTraceAndSlog t) {
    /**
      -------------------  Add the following code block --------------------
    */
  
    t.traceBegin("StartDeviceSpecificServices: AmbientLight");
    final String[] classes = mSystemContext.getResources().getStringArray(
            R.array.config_deviceSpecificSystemServices);

    Slog.i(TAG, "AmbientLight: DEBUG: Found " + (classes != null ? classes.length : 0) + " device-specific services.");
    for (final String className : classes) {
        Slog.i(TAG, "AmbientLight: DEBUG: Attempting to start -> " + className);
        t.traceBegin("AmbientLight: StartDeviceSpecificServices " + className);
        try {
            mSystemServiceManager.startService(className);
        } catch (Throwable e) {
            reportWtf("AmbientLight: caught error " + className, e);
        }
        t.traceEnd();
    }
    t.traceEnd();
  
   /**
      ------------------- code block --------------------
   */
}
