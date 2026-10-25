# AOSP Custom Framework System Service — SSI, GSI & Treble Compliant

- **Ambient Light Service** (`ambient_light_service`)  
- A production-grade, modular custom system service that lives entirely outside `frameworks/base`.  
- Fully compatible with **Shared System Image (SSI)**, **Generic System Image (GSI)**, and **Project Treble**.

[![AOSP](https://img.shields.io/badge/AOSP-android--latest--release-green)](https://source.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1+-purple)](https://kotlinlang.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Treble](https://img.shields.io/badge/Treble-Compliant-success)](https://source.android.com/docs/core/architecture/treble)
[![SSI](https://img.shields.io/badge/SSI%2FGSI-Ready-blue)](https://source.android.com/docs/core/architecture/partitions/shared-system-image)

---

## Why This Project Exists

This repository demonstrates the **correct, modern way** to ship a custom framework service:

| Traditional Approach                    | This Project (SSI / Treble)             |
| --------------------------------------- | --------------------------------------- |
| Patch `frameworks/base`                 | Zero changes to `frameworks/base`       |
| Service lives in `system` partition     | Service lives in `system_ext`           |
| Hard-coded registration in SystemServer | Device-specific initializer via overlay |
| Breaks GSI / SSI                        | Fully GSI / SSI / Treble compliant      |

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              Client Applications                             │
│                    (Platform apps / Privileged apps / Shell)                 │
└────────────────────────────────┬────────────────────────────────────────────┘
                                 │ Binder IPC
                                 │ Context.getSystemService()  or  ServiceManager
                                 ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                    AmbientLightManager (SDK Layer)                           │
│              device/.../AmbientLightService/sdk/                             │
│         • Type-safe Kotlin API                                               │
│         • Listener registration with Executor support                        │
│         • Automatic death recipient handling                                 │
└────────────────────────────────┬────────────────────────────────────────────┘
                                 │ IAmbientLightService.Stub / Proxy
                                 ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                 AmbientLightService (System Server Process)                  │
│              device/.../AmbientLightService/service/                         │
│         • Runs inside system_server (UID 1000)                               │
│         • RemoteCallbackList for multi-client listeners                      │
│         • Per-UID listener quota + permission enforcement                    │
│         • dumpsys + shell command interface                                  │
└────────────────────────────────┬────────────────────────────────────────────┘
                                 │ publishBinderService()
                                 ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                    AmbientLightInitializer (SystemService)                   │
│         Registered via config_deviceSpecificSystemServices overlay           │
│         (No modification of SystemServer.java required)                      │
└─────────────────────────────────────────────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         system_ext partition                                 │
│   • AIDL interface (com.darshan.ambientlight)                                │
│   • Service JAR + SDK JAR                                                    │
│   • SELinux policies (plat_private)                                          │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Key Design Decisions

1. **`system_ext` placement** — The service, AIDL, and SDK are marked `system_ext_specific: true`. This keeps the pure AOSP `/system` image untouched.
2. **Device-specific registration** — Instead of editing `SystemServer.java`, the service is started via the official `config_deviceSpecificSystemServices` overlay mechanism.
3. **Standalone SystemServer JAR** — The service is added through `PRODUCT_STANDALONE_SYSTEM_SERVER_JARS` + `PRODUCT_SYSTEM_SERVER_JARS`.
4. **Stable AIDL** — Interface lives under `com.darshan.ambientlight` with proper Java backend generation.
5. **Strict SELinux** — Custom service type `ambient_light_service` with minimal `find`/`add` permissions.

---

## Repository Layout

```
device/darshan/
├── cf_x86_64/                          # Cuttlefish device tree (x86_64)
│   ├── AndroidProducts.mk
│   ├── BoardConfig.mk                  # Adds SELinux policy dirs
│   ├── aosp_darshan_cf.mk
│   ├── device.mk                       # Packages + system_server jars
│   └── overlay/
│       └── frameworks/base/core/res/res/values/config.xml
│
└── common/frameworks/AmbientLightService/
    ├── aidl/                           # Stable AIDL interface
    │   ├── Android.bp
    │   └── com/darshan/ambientlight/
    │       ├── IAmbientLightService.aidl
    │       └── IAmbientLightListener.aidl
    │
    ├── sdk/                            # Client-facing manager
    │   ├── Android.bp
    │   ├── proguard.flags
    │   └── src/.../AmbientLightManager.kt
    │
    ├── service/                        # System service implementation
    │   ├── Android.bp
    │   └── src/.../
    │       ├── AmbientLightService.kt
    │       └── AmbientLightInitializer.kt
    │
    └── sepolicy/plat_private/          # SELinux policies
        ├── service.te
        ├── service_contexts
        ├── file_contexts
        └── shell.te
```

---

## Features

- Full Binder IPC with asynchronous callbacks (`RemoteCallbackList`)
- Permission-protected API (`com.darshan.permission.ACCESS_AMBIENT_LIGHT`)
- Per-UID listener quota to prevent abuse
- `dumpsys ambient_light_service` support
- Shell command interface (`cmd ambient_light_service`)
- Kotlin-first implementation with modern concurrency patterns
- Zero modifications to AOSP core frameworks
- Ready for multi-device SSI reuse

---

## Build & Run (Cuttlefish)

### 1. Prerequisites

- AOSP tree based on `android-latest-release` (or equivalent)
- Linux host with KVM support
- Sufficient disk space (~200 GB recommended)

### 2. Place the device tree

```bash
# From AOSP root
cp -r /path/to/this/repo/device/darshan device/
```

### 3. Lunch & Build

```bash
source build/envsetup.sh
lunch aosp_darshan_cf-trunk_staging-userdebug

# Full build (or incremental)
m
```

### 4. Launch Cuttlefish

```bash
launch_cvd
```

### 5. Connect

```bash
adb connect 127.0.0.1:6520

#Optionally, on a new terminal window
scrcpy -s 127.0.0.1:6520
```

---

## Verification

### SELinux status
```bash
adb -s 127.0.0.1:6520 shell getenforce
# Output: → Enforcing
```

### Service registration
```bash
adb -s 127.0.0.1:6520 shell service check ambient_light_service
# Output: → Service ambient_light_service: found

adb -s 127.0.0.1:6520 shell service list | grep ambient_light
# Output: → N ambient_light_service: [com.darshan.ambientlight.IAmbientLightService]
```

### Runtime status
```bash
adb -s 127.0.0.1:6520 shell dumpsys ambient_light_service
# Output: --- AmbientLightService Status ---
            Current Lux Metric: 400
            Active Listeners: 0
          ----------------------------------------

```

### Shell commands
```bash
adb -s 127.0.0.1:6520 shell cmd ambient_light_service set 750
# Output: Successfully set ambient lux to 750

adb -s 127.0.0.1:6520 shell cmd ambient_light_service get
# Output: Current ambient lux: 750
```

---

## Using the SDK from an App

```kotlin
val manager = AmbientLightManager(context)

// Synchronous
val lux = manager.getLux()
manager.setLux(1200)

// Asynchronous
manager.registerListener(executor) { newLux ->
    Log.i("ALS", "Lux changed: $newLux")
}
```

> The SDK JAR is installed on `system_ext` and can be linked by privileged / platform-signed applications.

---

## SELinux Summary

| Type                        | Domain          | Permission      |
|-----------------------------|-----------------|-----------------|
| `ambient_light_service`     | `system_server` | `add`, `find`   |
| `ambient_light_service`     | `platform_app`  | `find`          |
| `ambient_light_service`     | `system_app`    | `find`          |
| `ambient_light_service`     | `shell`         | `find`          |

Policies live under `system_ext` private sepolicy and are automatically included via `SYSTEM_EXT_PRIVATE_SEPOLICY_DIRS`.

---

## Comparison with Classic Approach

| Aspect                        | Classic (`frameworks/base`) | This Repo (SSI)          |
|-------------------------------|-----------------------------|--------------------------|
| Modifies `SystemServer.java`  | Yes                         | No                       |
| Modifies `SystemServiceRegistry` | Yes                      | No                       |
| Partition                     | `/system`                   | `/system_ext`            |
| GSI compatible                | No                          | Yes                      |
| SSI reusable                  | No                          | Yes                      |
| Treble compliant              | Partial                     | Full                     |
| Multi-device sharing           | Difficult                   | Designed for it          |

---

## Related Projects

- [AOSP-AmbientLight-SystemService](https://github.com/darshan-miskin/AOSP-AmbientLight-SystemService) — Classic (non-SSI) implementation that modifies `frameworks/base` directly. Useful for learning the internal Binder registration path.

---

## License

```
Copyright (C) 2025 Darshan Miskin

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
```

---

**Author**: [Darshan Miskin](https://www.linkedin.com/in/darshan-miskin/)  
**Focus**: AOSP framework internals • System services • Treble / SSI architecture
