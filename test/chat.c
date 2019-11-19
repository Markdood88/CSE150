#include "syscall.h"
#include "stdio.h"

#define MAX_TEXT_SIZE 1000

char receivedText[MAX_TEXT_SIZE], sendText[MAX_TEXT_SIZE];

// Connects to the server specified in the first argument, reads from stdin and writes to stdout
int main(int argc, char* argv[]) {
	int host, socket;
  int  receivedEnd, sendEnd;
  int r, w; //r used to read, w used to write
  int done = 0; // 0 == false, 1 == true
	char lastByte;
	receivedEnd = 0;

  //check if host address is supplied
	if (argc != 2) {
    printf("Error: no host address\n");
    return 1;
  }

  //connect host to port 15, which is the chat server
	host = atoi(argv[1]);
	socket = connect(host, 15);

	printf("Sucessfully onnected to host %d\n", host);

  // read user input while input is not '.'
	while(!done)
  {
    // reset length of sendText
		sendEnd = 0;

		// read from input
		if ((r = read(stdin, sendText, 1)) == 1)
    {

			lastByte = sendText[0];
			sendEnd++;

      //try to read from stdin
			while (lastByte != '\n') {
        //d
				if ((r = read(stdin, sendText + sendEnd, 1)) == -1) {
					printf("Error reading from stdin \n");
          //end loop
					done = true;
					break;
				} else {

					sendEnd += r;
					lastByte = sendText[sendEnd - 1];

					// stop recording input if length exceeds MAX_TEXT_SIZE
					if (sendEnd == MAX_TEXT_SIZE - 1) {
						sendText[MAX_TEXT_SIZE - 1] = '\n';
						break;
					}
				}
			}

			// disconnect if input is '.'
			if (sendText[0] == '.' && sendText[1] == '\n') {
				printf("Exiting \n");
				break;
			} else if(sendText[0] != '\n') {
        //send this message to server and display to other clients
				w = write(socket, sendText, sendEnd);

        //server not responding
				if (w == -1) {
					printf("Error: Server not responding.\n");
					break;
				}
			}
		}

		// read from chat server
		r = read(socket, receivedText + receivedEnd, 1);
		if (r == 1) {
			lastByte = receivedText[receivedEnd++];
      // new line, write out line
			if (lastByte == '\n') {
				w = write(stdout, receivedText, receivedEnd);
				// Reset the receivedText string for more input from socket
				receivedEnd = 0;
			}
		} else if (r == -1) {
      //server not responding
			printf("Error: server not responding.\n");
			break;
		}
	}

  //successfully disconnects from server
	close(socket);

	return 0;
}
