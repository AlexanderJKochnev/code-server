# Kotlin projects directory
## создание gradlew (выполнять в code-server терминале из директории проекта)
gradle wrapper --gradle-version 8.10.2
## сборка проекта (выполнять в code-server терминале из директории проекта)
./gradlew assembleDebug

## После успешного выполнения ./gradlew assembleDebug файл лежит по пути:
~/projects/PhotoSender/app/build/outputs/apk/debug/app-debug.apk

# production
## Шаг 2. Создать ключ для подписи

### В терминале (не в code-server, а локально или в контейнере с JDK):
keytool -genkey -v -keystore ~/photo-uploader-release.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias photo-uploader













## сборка production release
./gradlew assembleRelease

## (Опционально) Собрать AAB для Google Play

./gradlew bundleRelease