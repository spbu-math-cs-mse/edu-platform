# edu-platform

Цель проекта — создание телеграм-бота для облегчения коммуникации между учениками и преподавателями онлайн-школ. Ученик может оправлять решения задач в бота и получать через него же фидбэк по этим решениям, преподаватель — получать решения на проверку. Основные задачи:

- Помочь избежать большого количества чатов с учениками у преподавателя (и чатов с преподавателями у ученика).

- Если несколько преподавателей на одну группу учеников, не нужно чёткого разделения учеников между преподавателями — решение ученика проверяет не закреплённый за ним заранее преподаватель, а первый, который запросит решение на проверку. Так ученикам быстрее приходит фидбэк.

- Преподаватель может запрашивать решение, проверять его, отправлять фидбэк ученику и проставлять ему баллы в одном и том же чате с ботом. (Баллы автоматически проставляются ботом в гугл-таблицу). Всё это должно быть удобно делать, в том числе с телефона. Это упростит процесс проверки решений преподавателями и ускорит получение фидбэка учениками.

## Конфиг-файлы
Файл с конфигурацией лежит в `common-lib/src/main/resources/.env` и имеет структуру:
```
# The Telegram username for the student bot.
STUDENT_BOT_USERNAME=StudentBot

# A comma-separated list of Telegram User IDs for bot administrators.
ADMIN_IDS=123456789,987654321

# Paths to the files containing Telegram Bot Token for each bot.
STUDENT_BOT_TOKEN_FILE=./secrets/student_bot_token
TEACHER_BOT_TOKEN_FILE=./secrets/teacher_bot_token
ADMIN_BOT_TOKEN_FILE=./secrets/admin_bot_token
PARENT_BOT_TOKEN_FILE=./secrets/parent_bot_token

# Path to the JSON file containing the Google Service Account key.
GOOGLE_SHEETS_SERVICE_ACCOUNT_KEY_FILE=./secrets/google_api_key.json

# The JDBC connection URL for the PostgreSQL database.
# When running with Docker Compose, use the service name 'postgres_db'.
DB_URL=jdbc:postgresql://postgres_db:5432/edu-platform
# For running outside of Docker container use 'localhost'.
# DB_URL=jdbc:postgresql://localhost:5432/dachshund

# Database driver.
DB_DRIVER=org.postgresql.Driver

# Path to the file containing the database username.
DB_LOGIN_FILE=./secrets/db_user

# Path to the file containing the database password.
DB_PASSWORD_FILE=./secrets/db_password
```

Файл с ключом от сервисного аккаунта api googlesheets имеет структуру:
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
