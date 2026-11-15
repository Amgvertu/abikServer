# Веб-версия abik

## Локальный запуск

```bash
cd abik-web
npm ci
npm run dev -- --host 0.0.0.0 --port 7100
```

По умолчанию клиент использует `https://varamy.online/api/abik/`. Для работы в локальной сети выберите «192.168.0.119» на экране входа или создайте `.env.local` по образцу `.env.example`:

```env
VITE_API_URL=http://192.168.0.119:8083/api/abik/
```

Не добавляйте `.env.local` в систему контроля версий.

## Production-сборка

```bash
npm ci
npm run lint
npm run build
```

Готовые статические файлы находятся в `dist`. Веб-сервер должен отдавать `index.html` для всех неизвестных путей приложения (SPA fallback), но не для реальных статических файлов. Пример для Nginx:

```nginx
root /var/www/abik-web;
index index.html;

location / {
    try_files $uri $uri/ /index.html;
}

location /api/abik/ {
    proxy_pass http://127.0.0.1:8083/api/abik/;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
}
```

PWA и Web Push требуют HTTPS, кроме локальной разработки на `localhost`. `manifest.webmanifest`, `sw.js` и `/icons/` должны быть доступны из корня сайта.

## Web Push на backend

Браузерные подписки хранятся отдельно в `web_push_subscriptions`; миграция `V25__web_push_subscriptions.sql` применяется Flyway при запуске нового backend.

Сгенерируйте отдельную VAPID-пару:

```bash
node tools/generate-webpush-keys.mjs abik-server/config/webpush-local.env
```

Перенесите значения из созданного файла в защищённые переменные окружения production-сервиса:

```env
WEB_PUSH_ENABLED=true
WEB_PUSH_SUBJECT=mailto:admin@varamy.online
WEB_PUSH_PUBLIC_KEY=<публичный ключ>
WEB_PUSH_PRIVATE_KEY=<приватный ключ>
```

Не публикуйте приватный ключ и не помещайте его в веб-клиент. После настройки пересоберите и перезапустите backend, затем включите уведомления в разделе «Профиль» → «Настройки». API подписки:

- `GET /v1/web-push/public-key`;
- `POST /v1/web-push/subscriptions`;
- `DELETE /v1/web-push/subscriptions?endpoint=...`.

На Android и desktop уведомления поддерживаются браузером и установленной PWA. На iOS/iPadOS необходимо установить PWA на экран «Домой» и разрешить уведомления внутри неё; обычная вкладка Safari не является эквивалентом установленной PWA.

## Ссылка на APK

Страница `/install` получает актуальную ссылку из `GET /v1/app/releases/latest`. Суперадминистратор может опубликовать версию на вкладке «Администрирование» → «Релиз APK».

Альтернатива для отдельной сборки — задать внешнюю HTTPS-ссылку перед `npm run build`:

```env
VITE_APK_DOWNLOAD_URL=https://<домен>/abik.apk
```

Значение из опубликованного релиза используется, если переменная не задана. Если релиза и переменной нет, интерфейс честно показывает, что ссылка ещё не опубликована.

## Проверки

В рабочей копии выполнены `npm run build`, `npm run lint` и backend `./gradlew test` (16 тестов). Для проверки реального входа и push необходимо запустить обновлённый backend и войти под существующей учётной записью.
