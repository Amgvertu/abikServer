# Развёртывание abik на production-сервере

Инструкция предназначена для обновления уже работающего сервера `varamy.online` и рассчитана на выполнение без опыта администрирования. Выполняйте разделы последовательно.

В инструкции используются фактические настройки проекта:

- серверный пользователь Linux: `amgvertu`;
- рабочая папка: `/home/amgvertu/projects/abik`;
- порт backend: `8083`;
- API prefix: `/api/abik`;
- база данных PostgreSQL: `abik`;
- пользователь базы данных: `user_abik`;
- production URL: `https://varamy.online/api/abik/`;
- WebSocket URL: `wss://varamy.online/api/abik/ws`;
- служба systemd: `abik-server.service`.

Если фактический пользователь или папка на сервере отличаются, замените их во всех командах. Не меняйте порт и `/api/abik`: Android-приложение рассчитано именно на эти значения.

## 1. Что понадобится

На компьютере с Windows:

1. Готовый файл `abik-server-0.0.1-SNAPSHOT.jar`.
2. Действующий файл `firebase-service-account.json`.
3. Программа WinSCP для копирования файлов на сервер.
4. Программа для SSH-подключения, например Windows Terminal или PuTTY.
5. Пароль пользователя базы данных `user_abik`.
6. Доступ к настройкам Nginx и systemd через `sudo`.

Не отправляйте `firebase-service-account.json`, пароль базы данных и JWT-секрет в мессенджеры, репозиторий или документацию.

## 2. Подключение к серверу

Подключитесь по SSH, заменив `IP_СЕРВЕРА` на реальный адрес:

```bash
ssh amgvertu@IP_СЕРВЕРА
```

Проверьте версию Java:

```bash
java -version
```

Должна быть Java **21 или новее**. Если команда не найдена или версия ниже 21, на Debian/Ubuntu попробуйте:

```bash
sudo apt update
sudo apt install -y openjdk-21-jre-headless
java -version
```

Если пакет отсутствует в репозитории операционной системы, остановитесь и уточните версию ОС командой `cat /etc/os-release`; не устанавливайте Java из непроверенных архивов.

## 3. Создание папок

Выполните:

```bash
BASE=/home/amgvertu/projects/abik
mkdir -p "$BASE/config" "$BASE/logs" "$BASE/uploads" "$BASE/backups"
chmod 700 "$BASE/config"
chmod 755 "$BASE/logs" "$BASE/uploads" "$BASE/backups"
```

Назначение папок:

- `config` — секретные настройки и Firebase service account;
- `logs` — журнал backend;
- `uploads` — аватары и логотипы;
- `backups` — резервные копии JAR и базы данных.

## 4. Резервная копия базы данных

Перед миграцией `V21` обязательно создайте резервную копию:

```bash
BASE=/home/amgvertu/projects/abik
STAMP=$(date +%Y%m%d-%H%M%S)
pg_dump -h 127.0.0.1 -U user_abik -W -F c -f "$BASE/backups/abik-before-v21-$STAMP.dump" abik
```

Команда запросит пароль пользователя `user_abik`. Во время ввода пароль не отображается — это нормально.

Проверьте, что файл создан и его размер не равен нулю:

```bash
ls -lh /home/amgvertu/projects/abik/backups/
```

Сохраните имя созданного `.dump`-файла.

## 5. Копирование JAR и Firebase на сервер

### 5.1. JAR

В WinSCP подключитесь к серверу как `amgvertu` и откройте:

```text
/home/amgvertu/projects/abik/
```

Скопируйте туда новый JAR под именем:

```text
abik-server-new.jar
```

Не перезаписывайте текущий JAR через WinSCP: сначала будет создана его резервная копия.

### 5.2. Firebase service account

Скопируйте `firebase-service-account.json` в:

```text
/home/amgvertu/projects/abik/config/firebase-service-account.json
```

Затем в SSH выполните:

```bash
chmod 600 /home/amgvertu/projects/abik/config/firebase-service-account.json
chown amgvertu:amgvertu /home/amgvertu/projects/abik/config/firebase-service-account.json
```

Проверьте, что JSON не пустой:

```bash
wc -c /home/amgvertu/projects/abik/config/firebase-service-account.json
```

Размер должен быть больше нуля. Не выводите содержимое этого файла в терминал или журнал.

## 6. Защищённый файл с паролями

Создайте JWT-секрет:

```bash
openssl rand -hex 32
```

Скопируйте полученную строку. Она понадобится на следующем шаге.

> Если на production уже был настроен собственный `APP_JWT_SECRET`, сохраните прежнее значение. Смена JWT-секрета завершит существующие сессии, и пользователям потребуется снова войти в приложение.

Откройте файл окружения:

```bash
nano /home/amgvertu/projects/abik/config/abik-server.env
```

Вставьте две строки, заменив значения в кавычках:

```ini
SPRING_DATASOURCE_PASSWORD="ПАРОЛЬ_ПОЛЬЗОВАТЕЛЯ_user_abik"
APP_JWT_SECRET="СГЕНЕРИРОВАННАЯ_СТРОКА_OPENSSL"
```

Сохраните файл: `Ctrl+O`, затем `Enter`; выйдите: `Ctrl+X`.

Ограничьте доступ:

```bash
chmod 600 /home/amgvertu/projects/abik/config/abik-server.env
chown amgvertu:amgvertu /home/amgvertu/projects/abik/config/abik-server.env
```

Переменные окружения имеют приоритет над значениями внутри JAR. Поэтому реальный пароль базы данных не нужно вписывать в исходники.

## 7. Настройка systemd

Создайте резервную копию текущей службы, если она уже существует:

```bash
if [ -f /etc/systemd/system/abik-server.service ]; then
  sudo cp /etc/systemd/system/abik-server.service "/etc/systemd/system/abik-server.service.backup-$(date +%Y%m%d-%H%M%S)"
fi
```

Откройте файл службы:

```bash
sudo nano /etc/systemd/system/abik-server.service
```

Вставьте конфигурацию целиком:

```ini
[Unit]
Description=abik backend
After=network-online.target postgresql.service
Wants=network-online.target

[Service]
Type=simple
User=amgvertu
WorkingDirectory=/home/amgvertu/projects/abik
EnvironmentFile=/home/amgvertu/projects/abik/config/abik-server.env
ExecStart=/usr/bin/java -jar /home/amgvertu/projects/abik/abik-server.jar --spring.profiles.active=prod
Restart=on-failure
RestartSec=10
TimeoutStopSec=30
SuccessExitStatus=143
StandardOutput=journal
StandardError=journal
SyslogIdentifier=abik-server

[Install]
WantedBy=multi-user.target
```

Проверьте путь к Java:

```bash
which java
```

Если команда вернула не `/usr/bin/java`, замените путь в `ExecStart` на фактический.

Сохраните файл и примените конфигурацию:

```bash
sudo systemctl daemon-reload
sudo systemctl enable abik-server
```

## 8. Установка нового JAR и запуск

Остановите backend:

```bash
sudo systemctl stop abik-server
```

Сохраните предыдущий JAR, если он существует:

```bash
BASE=/home/amgvertu/projects/abik
STAMP=$(date +%Y%m%d-%H%M%S)
if [ -f "$BASE/abik-server.jar" ]; then
  cp -a "$BASE/abik-server.jar" "$BASE/backups/abik-server-$STAMP.jar"
fi
```

Установите новый файл:

```bash
BASE=/home/amgvertu/projects/abik
mv "$BASE/abik-server-new.jar" "$BASE/abik-server.jar"
chown amgvertu:amgvertu "$BASE/abik-server.jar"
chmod 750 "$BASE/abik-server.jar"
```

Если `mv` сообщает, что `abik-server-new.jar` отсутствует, не запускайте сервер: вернитесь к разделу 5 и скопируйте JAR.

Запустите backend:

```bash
sudo systemctl start abik-server
sudo systemctl status abik-server --no-pager
```

Статус должен быть `active (running)`. Выход из просмотра статуса: `q`.

## 9. Проверка миграции V21

Миграция применяется автоматически при запуске. **Не выполняйте `V21__guide_releases_expenses.sql` вручную**: Flyway сам создаст новые таблицы и зарегистрирует версию миграции.

Посмотрите журнал:

```bash
sudo journalctl -u abik-server -n 150 --no-pager
```

При успешном запуске не должно быть ошибок Flyway, PostgreSQL или `APPLICATION FAILED TO START`.

Дополнительно проверьте последние сообщения о миграциях:

```bash
sudo journalctl -u abik-server --no-pager | grep -Ei 'flyway|migrat|started|error' | tail -n 50
```

`V21` добавляет таблицы `guide_progress`, `app_releases` и `team_expenses`.

Проверить их наличие можно через PostgreSQL:

```bash
psql -h 127.0.0.1 -U user_abik -d abik -c "SELECT tablename FROM pg_tables WHERE schemaname = 'public' AND tablename IN ('guide_progress', 'app_releases', 'team_expenses') ORDER BY tablename;"
```

Введите пароль базы данных. В результате должны отображаться все три таблицы.

## 10. Проверка backend без Nginx

На самом сервере выполните:

```bash
curl --fail --silent --show-error http://127.0.0.1:8083/api/abik/v1/health
```

Ожидаемый ответ содержит:

```json
{"status":"ok","service":"abik-server"}
```

Если здесь возникает ошибка, Nginx пока не настраивайте: сначала проверьте журнал службы.

## 11. Настройка Nginx

### 11.1. Найдите существующий HTTPS-конфиг

```bash
sudo grep -R -n 'server_name.*varamy.online' /etc/nginx/sites-enabled /etc/nginx/conf.d
```

Задайте путь к найденному файлу в переменной `NGINX_CONF`, заменив пример ниже на фактический путь. Если найден симлинк из `sites-enabled`, укажите соответствующий файл в `sites-available`.

```bash
NGINX_CONF=/etc/nginx/sites-available/ИМЯ_ФАЙЛА
```

Сделайте резервную копию и откройте конфигурацию:

```bash
sudo cp "$NGINX_CONF" "$NGINX_CONF.backup-before-abik-ws"
sudo nano "$NGINX_CONF"
```

### 11.2. Добавьте location-блоки

Внутри **существующего HTTPS** `server { ... }` для `varamy.online` добавьте два блока. Не удаляйте `listen`, `server_name`, SSL-сертификаты и другие существующие настройки.

```nginx
# WebSocket для обновления открытых экранов Android.
location = /api/abik/ws {
    proxy_pass http://127.0.0.1:8083;
    proxy_http_version 1.1;

    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;

    proxy_read_timeout 3600s;
    proxy_send_timeout 3600s;
}

# REST API, авторизация, файлы, расходы, Excel и остальные запросы.
location /api/abik/ {
    proxy_pass http://127.0.0.1:8083;
    proxy_http_version 1.1;

    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;

    client_max_body_size 6m;
    proxy_read_timeout 120s;
}
```

Важно: в обоих `proxy_pass` после `8083` **нет завершающего `/`**. Так Nginx сохраняет исходный путь `/api/abik/...`. Завершающий слэш в данной конфигурации может удалить необходимый prefix и вызвать ошибки 404.

Не создавайте второй `server` для того же `server_name`: используйте существующий HTTPS-блок.

### 11.3. Проверьте и примените конфигурацию

```bash
sudo nginx -t
```

Должны быть сообщения `syntax is ok` и `test is successful`. Только после этого выполните:

```bash
sudo systemctl reload nginx
sudo systemctl status nginx --no-pager
```

Если `nginx -t` показал ошибку, не перезагружайте Nginx. Исправьте указанную строку или восстановите резервную копию конфигурации.

## 12. Проверка production API

С любого компьютера, имеющего доступ к `varamy.online`, выполните:

```bash
curl --fail --silent --show-error https://varamy.online/api/abik/v1/health
```

Ответ также должен содержать `"status":"ok"` и `"service":"abik-server"`.

Из Windows PowerShell можно проверить так:

```powershell
Invoke-RestMethod -Uri 'https://varamy.online/api/abik/v1/health'
```

Дополнительно откройте Swagger:

```text
https://varamy.online/api/abik/swagger-ui/index.html
```

Если Swagger отключён production-настройками, это не является ошибкой; основная проверка — `/v1/health` и работа приложения.

## 13. Проверка WebSocket

Обычный браузер не подходит для проверки `/ws`: сервер требует JWT access token в query-параметре `token`.

Проверка выполняется через приложение:

1. Установите новый APK на два устройства или эмулятора.
2. Выберите сервер `varamy.online`.
3. Войдите под двумя аккаунтами, имеющими доступ к одной команде.
4. Откройте на втором устройстве экран команды или событий.
5. На первом устройстве измените доступное событие или состав.
6. Убедитесь, что открытый экран второго устройства обновился без полного перезапуска приложения.

Если данные не обновляются, проверьте:

- наличие отдельного `location = /api/abik/ws`;
- заголовки `Upgrade` и `Connection`;
- отсутствие завершающего `/` в `proxy_pass`;
- отсутствие ошибок подключения в `sudo journalctl -u abik-server -n 100 --no-pager`.

Кратковременные переподключения WebSocket допустимы: Android автоматически восстанавливает соединение.

## 14. Проверка основных функций

Под реальными тестовыми аккаунтами проверьте:

1. Авторизацию через `varamy.online`.
2. Загрузку и отображение аватара.
3. Создание и просмотр расхода пользователем с соответствующей ролью.
4. Отсутствие доступа к изменению расходов у обычного участника.
5. Выгрузку всей команды в Excel.
6. Выгрузку отдельного именованного списка в Excel.
7. Прохождение обучающих подсказок и сохранение их состояния после повторного входа.
8. Отображение пользователей, команд и статистики в админке суперпользователя.

Для проверки обновлений приложения:

1. Разместите APK на HTTPS-сервере так, чтобы файл скачивался по прямой ссылке без авторизации.
2. Проверьте ссылку в браузере или командой `curl -I https://.../app.apk`.
3. В админке суперпользователя опубликуйте версию с правильными `versionCode`, `versionName`, примечаниями и HTTPS-ссылкой.
4. На устройстве с более старой версией проверьте появление диалога обновления.

Не используйте локальный `http://192.168.0.119` в публикуемой ссылке на APK.

## 15. Обновление backend в следующий раз

Для последующих обновлений:

1. Сделайте резервную копию базы данных.
2. Загрузите новый JAR как `abik-server-new.jar`.
3. Сохраните прежний `abik-server.jar` в `backups`.
4. Выполните `sudo systemctl stop abik-server`.
5. Переименуйте новый JAR в `abik-server.jar`.
6. Выполните `sudo systemctl start abik-server`.
7. Проверьте журнал, миграции и `/api/abik/v1/health`.

Не удаляйте папки `uploads`, `config` и `backups` при обновлении.

## 16. Откат

Если новый backend не запускается:

```bash
sudo systemctl stop abik-server
```

Скопируйте предыдущий JAR из `backups` обратно:

```bash
BASE=/home/amgvertu/projects/abik
cp -a "$BASE/backups/ИМЯ_ПРЕДЫДУЩЕГО_ФАЙЛА.jar" "$BASE/abik-server.jar"
sudo systemctl start abik-server
sudo systemctl status abik-server --no-pager
```

Таблицы из `V21` добавляются отдельно и не удаляют старые данные. Обычно при откате JAR восстанавливать базу не требуется.

Полное восстановление `.dump` перезапишет более новые изменения в базе. Выполняйте его только после отдельного анализа проблемы и создания копии текущего состояния.

## 17. Типичные ошибки

### `502 Bad Gateway`

Backend не запущен или Nginx обращается не к тому порту:

```bash
sudo systemctl status abik-server --no-pager
sudo journalctl -u abik-server -n 150 --no-pager
curl -v http://127.0.0.1:8083/api/abik/v1/health
```

### `404 Not Found`

Проверьте:

- `/api/abik/` в адресе запроса;
- `proxy_pass http://127.0.0.1:8083;` без завершающего слэша;
- `--spring.profiles.active=prod` в `ExecStart`.

### Приложение возвращает «Неавторизован»

Проверьте логин и пароль, выбранный сервер и доступность `/v1/health`. Не отключайте JWT или проверку авторизации ради устранения ошибки.

### Не работает FCM

Проверьте наличие и права файла:

```bash
ls -l /home/amgvertu/projects/abik/config/firebase-service-account.json
sudo journalctl -u abik-server --no-pager | grep -i 'firebase\|fcm' | tail -n 50
```

Файл должен принадлежать `amgvertu` и быть доступен сервису для чтения.

### Ошибка миграции

Не удаляйте строки из `flyway_schema_history` и не меняйте уже применённые SQL-файлы. Сохраните текст ошибки:

```bash
sudo journalctl -u abik-server -n 200 --no-pager
```

После исправления причины перезапустите службу. PostgreSQL и Flyway обычно откатывают неудачную миграцию внутри транзакции.
