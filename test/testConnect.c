#include "syscall.h"

int main(int argc, char** argv)
{
	if(argc!=2)
	{

    	printf("Usage: connect num num\n");
		return 0;
	}
	connect(1,1);
	return 1;

}