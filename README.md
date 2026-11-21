# code-server
code-server + JDK 17 + Android CLI

## перед перевым:
source .env
mkdir -p "/mnt/hdd_data/@named_volumes/code_server"/{projects,gradle-cache,android-sdk,code-config}
sudo chown -R 1000:1000 "/mnt/hdd_data/@named_volumes/code_server"

###  проверка статуса эмулятора
docker exec -it android-emulator cat device_status
docker exec -it android-emulator adb shell getprop sys.boot_completed

### запуск эмулятора вручную
emulator -list-avds
samsung_galaxy_s10_11.0
emulator -avd samsung_galaxy_s10_11.0 -no-snapshot


### удаление блокировок вручную
    rm -f ~/.android/avd/samsung_galaxy_s10_11.0.avd/*.lock
    ps aux | grep -E "qemu|emulator"
    kill -9 <PID>   #  удаление оставшися провессов

#### запуск заново
    emulator -avd samsung_galaxy_s10_11.0 -no-snapshot -no-audio -gpu swiftshader_indirect
    

## соединение с эмулятором
    docker compose exec -it android-ide sh
    adb start-server
    adb connect android-emulator:5555
    adb devices
