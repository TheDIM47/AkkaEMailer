### Akka E-mailer sample application

Sample application that listening RabbitMQ exchange for commands (json) and send email messages.

Expected RabbitMQ messages are:

Welcome message:
```
{
  "id": "35ec8679-d158-4761-9f94-f0c438ad6f78",
  "type": "welcome", 
  "recipient": "obi-wan@rootservices.org",
  "first_name": "obi-wan",
  "target": "https://rootservices.org/verify"
}
```
Reset password message:
```
{
  "id": "35ec8679-d158-4761-9f94-f0c438ad6f78",
  "type": "reset_password", 
  "recipient": "obi-wan@rootservices.org",
  "first_name": "obi-wan",
  "target": "https://rootservices.org/verify"
}
```

RMQHandler - typed actor listening MQ and fire RMQActor events

RMQActor handles received MQ messages and start MailerActor for each message.

MailerActor loads email template, smtp and message properties, create message and send to mail server. 
After sending message, actor send Ack back to RMQActor and die.

RMQActor send Ack to RabbitMQ exchange.

#### Simple emailing

You can send messages with MessageSupervisor actor using typed "Welcome" and "ResetPassword" messages.

#### Using from Java

See src/main/java/app/JavaMain.java

See src/main/java/app/SimpleJavaMain.java

#### Compile and run

Edit src/main/resources/application.conf, then run
```
$sbt run
```
Assembly to fat jar:
```
$sbt assembly
$java -jar target/scala-2.12/akka-emailer-1.0-SNAPSHOT.jar
```
