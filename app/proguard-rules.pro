# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in the Android SDK tools proguard configuration.

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
