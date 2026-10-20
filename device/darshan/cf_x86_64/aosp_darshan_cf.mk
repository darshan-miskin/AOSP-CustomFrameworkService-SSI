$(call inherit-product, device/google/cuttlefish/vsoc_x86_64_only/phone/aosp_cf.mk)

PRODUCT_NAME := aosp_darshan_cf
PRODUCT_DEVICE := cf_x86_64
PRODUCT_BRAND := Darshan
PRODUCT_MODEL := Darshan Custom Virtual Device

$(call inherit-product, device/darshan/cf_x86_64/device.mk)