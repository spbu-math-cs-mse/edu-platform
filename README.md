# edu-platform

Цель проекта — создание телеграм-бота для облегчения коммуникации между учениками и преподавателями онлайн-школ. Ученик может оправлять решения задач в бота и получать через него же фидбэк по этим решениям, преподаватель — получать решения на проверку. Основные задачи:

- Помочь избежать большого количества чатов с учениками у преподавателя (и чатов с преподавателями у ученика).

- Если несколько преподавателей на одну группу учеников, не нужно чёткого разделения учеников между преподавателями — решение ученика проверяет не закреплённый за ним заранее преподаватель, а первый, который запросит решение на проверку. Так ученикам быстрее приходит фидбэк.

- Преподаватель может запрашивать решение, проверять его, отправлять фидбэк ученику и проставлять ему баллы в одном и том же чате с ботом. (Баллы автоматически проставляются ботом в гугл-таблицу). Всё это должно быть удобно делать, в том числе с телефона. Это упростит процесс проверки решений преподавателями и ускорит получение фидбэка учениками.

## Конфиг-файлы
Файл с конфигурацией бд лежит в `common-lib/src/main/resources/config.json` имеет структуру:
```json
{
  "googleSheetsConfig": {
    "serviceAccountKey": "google_api_key.json",
    "spreadsheetId": "SPREADSHEET_ID_12345"
  },
  "databaseConfig": {
    "url": "jdbc:postgresql://localhost:<port>/<database>",
    "driver": "org.postgresql.Driver",
    "login": "<login>",
    "password": "<password>"
  },
  "redisConfig" : {
    "host": "localhost",
    "port": "6379"
  }
}
```

Файл с ключом от сервисного аккаунта api googlesheets лежит в `common-lib/src/main/resources/google_api_key.json` и имеет структуру:
```json
{
  "type": "service_account",
  "project_id": "<project_id>",
  "private_key_id": "<private_key_id>",
  "private_key": "-----BEGIN PRIVATE KEY-----\n<private key>\n-----END PRIVATE KEY-----\n",
  "client_email": "<client_email>",
  "client_id": "<client_id>",
  "auth_uri": "https://accounts.google.com/o/oauth2/auth",
  "token_uri": "https://oauth2.googleapis.com/token",
  "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
  "client_x509_cert_url": "https://www.googleapis.com/robot/v1/metadata",
  "universe_domain": "googleapis.com"
}

```


## Detekt

В проекте используется [detekt](https://detekt.dev), утилита для статического анализа кода на Kotlin.
В каждом подмодуле определены gradle функции `detektMain` и `detektTest`, которые и отвечают за статический анализ; их можно запускать для локальной проверки и они же запускаются на CI. 
Не используйте другие таски с именем `detekt` в них.

Конфигурационный файл лежит в [config/detekt/detekt.yml](config/detekt/detekt.yml), этим файлом пользуются все подмодули.

Чтобы отформатировать код, нужно запустить gradle таску `spotlessApply`

Файлы `detekt-baseline-.*` в подмодулях содержат старые ошибки, которые будут со временем исправлять, но пока они игнорируются. **Их не надо изменять руками и без хорошей на то причины**. Подробнее прочитать про них можно в документации [detekt](https://detekt.dev).
