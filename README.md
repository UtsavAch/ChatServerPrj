# Porject for Communication Networks CC3002

The project consists of developing a chat server in Java and a simple client to communicate with it. 

## Server (50%)
- The server must be based on the multiplex model, and it is advisable to use the program developed in exercise sheet nº 5 of the practical classes as a starting point 
- The server is implemented in a class named **ChatServer**
- It must accept as an argument from the command line the TCP port number, for example: `java ChatServer 8000`

## Client (35%)
- The client must use two threads, to be able to receive messages from the server while waiting for the user to write the next message or command ***(otherwise it would block reading the socket, making the interface inoperative)***
- The client is implemented in a class named **ChatClient**
- It must accept as an argument from the command line the DNS name of the server, of which it comunicates to and the TCP port number, for example: `java ChatClient localhost 8000`

## Protocol
- The communication protocol is text line oriented, each message sent from the client to the server, or vice-versa, must end with newline `\n` <ins> ***but not the message itself must not contain newline*** </ins>
- It is up for the server to do buffering for the partialy recieved client's messages
- The messages sent from the client to the server can be simple commands or simple messages
- Commands are like `/<command>` and can have arguments, `/<command> <arg>`,
- Simple messages ***can only*** be sent when a a user is in a chat room
-  `/` must be acounted only in the beggining, for example a user joe writes `/notacommand` we must account that is not a command and the client must send to the server `//notacommand` and the server sends `MESSAGE joe /notacommand`

## Commands
- `/nick <name>` used to chose a name or change it if possible
- `/join <room>` used to enter chat room named room, creates one if does not exist
- `/leave` leaves the room where the user is
- `/bye` leave the chat
- `/priv <name> <message>` user sends to <name> a message and ***only him*** BONUS (10% with message)
  
## Messages 
### Server -> Client
- `OK` used to indicate successful command
- `ERROR` used to indicate insuccessful command
- `MESSAGE <name> <message>` to send a message to the users in same room
- `NEWNICK <former_name> <new_name>` indicate to the users in the room that <former_name> chaged to <new_name>
- `JOINED <name>` users in room see new user in the room
- `LEFT <name>` similar to the previous example, but user left
- `BYE` used to confirm the user used `/bye`
- `PRIVATE <sender> <message>` user receives private message from sender BONUS (10% with command)
- We later can pretify this messages in the way is makes it like for example `NEWNICK <former_name> <new_name>` to `<former_name> changed name to <new_name>` BONUS (5%)

## States of client
### Server keeps associated with every client a state info, which can be the foolowing:
- `init` initial state of a user who just connected with the server, ***has no name (nikname)!***
- `outside` user has a name but not in any chat room
- `inside` user is in chat room, and ***must be treated as such***

