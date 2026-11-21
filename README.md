# code-server
code-server + JDK 17 + Android CLI

## перед перевым:
source .env
mkdir -p "/mnt/hdd_data/@named_volumes/code_server"/{projects,gradle-cache,android-sdk,code-config}
sudo chown -R 1000:1000 "/mnt/hdd_data/@named_volumes/code_server"

## соединение с эмулятором
### docker compose exec -it android-ide sh
    adb start-server
    adb connect android-emulator:5555
    adb devices
