/**
  Other SystemServer variables and functions
*/
  
private void startOtherServices(@NonNull TimingsTraceAndSlog t) {
    /**
      -------------------  Add the following code block --------------------
    */
  
    try {
          Slog.i("Darshan", "DEBUG: Attempting to start AmbientLightInitializer directly");
          mSystemServiceManager.startService("com.darshan.server.ambientlight.AmbientLightInitializer");
          Slog.i("Darshan", "DEBUG: Successfully initiated AmbientLightInitializer");
    } catch (Throwable e) {
        Slog.wtf("Darshan", "DEBUG: Failed to start AmbientLightInitializer", e);
    }
  
   /**
      ------------------- code block --------------------
   */
}
