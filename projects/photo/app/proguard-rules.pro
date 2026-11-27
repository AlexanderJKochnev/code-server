-keep class com.example.photouploader.data.model.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# Сохраняем generic-подписи для Call и Response (R8 full mode их вырезает)
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# Сохраняем Continuation — используется в suspend-функциях Retrofit
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# Сохраняем атрибуты подписи и аннотаций (нужно для Gson)
-keepattributes Signature
-keepattributes *Annotation*

-keep class com.example.photouploader.data.model.** { *; }

