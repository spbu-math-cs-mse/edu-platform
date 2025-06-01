# Scheduled message interface

## General statements
AdminApi interface should be used for interacting with the app.

Per single telegram update, a single state should be responsible for user input.
BotStateWithHandlers provides an interface for that.
Please analyze its interface and dependencies.
Good examples of this is QueryAssignmentForCheckingGradesState of student bot.
Please consider at least 3 other implementors for seeing examples.

kotlin-result library Result<Value, Error> should be used everywhere.
It provides monadic error handling support and is used extensively in the project instead of exceptions.
Its `binding` and `coroutineBinding` methods are especially useful, as well as other monadic operations.
Do not forget to import its methods properly, as they clash with kotlin's builtin Result class, which we don't use at all.

You need to implement states so the administator could, using admin bot, could view recently added scheduled messages and delete the messages by their ids.
This corresponds to methods AdminApi::viewSentMessages and AdminApi::deleteScheduledMessage.
AdminApi::resolveScheduledMessage might also be useful.

The bot communicates in russian.

If at any point an error occured, the bot should notify the user and return to the menu state.
The error should be logged.

## Details
### Recent messages
The bot should query the course from the admin.
As a reference, see QueryCourseForCheckingDeadlinesState class of student bot.

After the course is selected, the number of last scheduled message should be asked, with 5 displayed as the recommended number.
The number should be entered by text.
The number should be less than zero and less then 100.
If the number is not recognized, ask again and explain the error whenever possible.

After that the user should be asked with an inline keyboard (yes/no) press whether full text of messages needs to be printed.

After that, the list of formatted messages would appear corresponding to the last sent messages.
Make the output pretty using emojis (but not excessively).
They must be sorted by the order from api call.
The message must provide all the information from the database except for images and full texts (if denied previously).
If the list is empty, a corresponding message must be reported.


### Delete message
Here, you must explain the role of the state and ask for an id of message to delete.
The id should be input explicitly.
If the id is invalid or unreconginzed, the user should be asked again.

After the id was asked, its content and full information should be pretyty printed and confirmation must be asked.
For an example of confirmation in states, see ConfirmSubmissionState class in student bot.


On confirmation, the message must be deleted and the user informed on the status.
After that the user should return to the MenuState.
