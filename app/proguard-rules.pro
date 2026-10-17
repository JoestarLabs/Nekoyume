# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile


# Google Hilt Dependency Injection
-keep,allowobfuscation,allowshrinking @dagger.hilt.EntryPoint class *
-keep,allowobfuscation,allowshrinking @dagger.hilt.GeneratesRootInput class *
-keepclassmembers class * {
    @javax.inject.Inject *;
}

# AndroidX Media3 Session & Playback Service
-keep public class com.bl4ckswordsman.nekoyume.core.audio.service.NekoyumePlaybackService {
    public <init>();
}
-keep class androidx.media3.session.MediaSessionService { *; }
-keep class androidx.media3.session.MediaSession { *; }
-keep interface androidx.media3.common.Player { *; }