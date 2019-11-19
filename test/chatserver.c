#include "syscall.h"
#include "stdio.h"

//chat server, connects at port 15 and connects a max of 16

#define MAX_TEXT_SIZE 1000

int clients[16]; //store clients, max is 16
char receivedText[MAX_TEXT_SIZE];

void displayMessages(int client) {
	int i;
  int w; // size of write
  int r; //size of read
  int receivedEnd = 0;
	char result[1];

	// read char input
	r = read(clients[client], result, 1);

  // return if no input
	if (r == 0) return;

	// disconnects client if
	if (r == -1) {
    printf("disconnecting client %d\n", client);
    close(clients[client]);
    clients[client] = -1;
    return;
  }

	// read input from client until input is == '/n'
	while ((r > -1) && (receivedEnd < MAX_TEXT_SIZE)) {
    //store input
		receivedText[receivedEnd++] = result[0];
    //end if '/n'
		if (result[0] == '\n') break;
    //keep reading until r <= -1
		r = read(clients[client], result, 1);
	}

  //end Abort if no text received
	if (receivedEnd == 0) return;

  receivedText[receivedEnd] = '\0';
  printf("broadcast: %s",receivedText);

	// If there was any text received from the client, display that message to every client
	for (i = 0; i < 16; ++i)
    //dont display to client that wrote msg or nonexistant clients
		if (i != client && clients[i] != -1) {
			w = write(clients[i], receivedText, receivedEnd);

			// If can't display msg, disconnect the client
			if (w != receivedEnd) {
				printf("Error while writing to client %d. Disconnecting client.", i);
				close(clients[i]);
				clients[i] = -1;
      }
		}
}

int main(int argc, char* argv[]) {
	int newSocket = 0;
	char result[1];

  //initialize client sockets to -1 meaning the server can accept new sockets
	for (int i = 0; i < 16; i++) {
		clients[i] = -1;
	}

  //loop until
	while (1) {

    //close server
		if (read(stdin, result, 1) != 0) {
			break;
		}

    // checks for new sockets
		newSocket = accept(15);

    // If new socket, add to client server list
		if (newSocket != -1)
    {
      //connects new socket and adds to list
      printf("client %d connected\n", newSocket);
			clients[newSocket] = newSocket;
		}

    //continue to display messges to all sockets in list
		for (i = 0; i < 16; i++) {
			if (clients[i] != -1) {
				displayMessages(i);
			}
		}
	}
}
