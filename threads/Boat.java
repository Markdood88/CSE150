package nachos.threads;
import nachos.ag.BoatGrader;
//last edited on 2/21 by Zach Light
public class Boat
{

	/* General Solution:
	 * Whenever more than one Kid is on Oahu they will sail to Molokai
	 * 1 Child will always take the boat back to Oahu
	 * Once 1 or fewer kids are on Oahu adults will start going to Molokai
	 * Each time 1 kid will bring the boat back
	 * This will result in more than 1 kid being on the island, making them sail back
	 * Once there are no adults, any remaining kid will sail back
	 * this will trigger the end using the communicator
	 */
    static BoatGrader bg;
    private final static boolean Oahu = false; //constants for knowing where people are
    private final static boolean Molokai = true;
    private static Lock boatLock = new Lock();//a lock representing who has use of the boat
    private static Condition2 onOahu = new Condition2(boatLock);//condition variables for people on each island
    private static Condition2 onMolokai = new Condition2(boatLock);
    private static Condition2 onBoat = new Condition2(boatLock);//condition variable to determine who is on the boat, only used in the case of kids
    private static Communicator endCon = new Communicator();//communicator used to determine once everyone has crossed
    private static int adultsOnOahu;//global variables to determine how many people of which type are where
    private static int childrenOnOahu;
    private static int totalOnMolokai;
    private static boolean finished;
    private static boolean boat;
    private static boolean driver;
    private static boolean rider;

    public static void selfTest()
    {
	BoatGrader b = new BoatGrader();
	
	System.out.println("\n ***Testing Boats with only 2 children***");
	begin(0, 2, b);
//  2/21 TODO:
//	Run test cases once Condidion2 is finished
//	If time is running out change condition2s to Condition and it should work the same

	//	System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
//  	begin(1, 2, b);
	
//	System.out.println("\n ***Testing Boats with 2 children, 0 adult***");
//	begin(0, 2, b);
	
//	System.out.println("\n ***Testing Boats with 3 children, 0 adult***");
//	begin(0, 3, b);
	
//	System.out.println("\n ***Testing Boats with 7 children, 1 adult***");
//	begin(1, 7, b);
	
//	System.out.println("\n ***Testing Boats with 50 children, 50 adult***");
//	begin(50, 50, b);
	
//	System.out.println("\n ***Testing Boats with 2 children, 12 adult***");
//	begin(12, 2, b);
	
//	System.out.println("\n ***Testing Boats with 3 children, 100 adult***");
//	begin(100, 3, b);

//  	System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
//  	begin(3, 3, b);
    }

    public static void begin( int adults, int children, BoatGrader b )
    {
	// Store the externally generated autograder in a class
	// variable to be accessible by children.
	bg = b;

	// Instantiate global variables here
	adultsOnOahu = adults;
	childrenOnOahu = children;
	totalOnMolokai = 0;
	finished = false;
	boat = Oahu;
	driver = false;
	rider = false;

	// Create threads here. See section 3.4 of the Nachos for Java
	// Walkthrough linked from the projects page.

	Runnable childR = new Runnable() {
	    public void run() {
                ChildItinerary();
            }
        };
	Runnable adultR = new Runnable() {
	    public void run() {
                AdultItinerary();
            }
        };
        for(int i = 0; i<adults;i++)
        {
        	KThread t = new KThread(adultR);
        	t.setName("Boat Thread: Adult #"+i);
        	t.fork();
        }

        for(int i = 0; i<children;i++)
        {
        	KThread t = new KThread(childR);
        	t.setName("Boat Thread: Child #"+i);
        	t.fork();
        }

        while(true)
        {
        	if(endCon.listen() == adults+children)
        	{
        		finished = true;
        		break;
        	}
        }
    }

    static void AdultItinerary()
    {
//	bg.initializeAdult(); //Required for autograder interface. Must be the first thing called.
	//DO NOT PUT ANYTHING ABOVE THIS LINE. 

	/* This is where you should put your solutions. Make calls
	   to the BoatGrader to show that it is synchronized. For
	   example:synchronized
	       bg.AdultRowToMolokai();
	   indicates that an adult has rowed the boat across to Molokai
	*/
		boolean loc = Oahu;//local variable for storing location of each individual
		boatLock.acquire();
		while(!finished)
		{
			if(loc == Oahu)//adult on Oahu
			{
				while(boat!=Oahu || childrenOnOahu>1||driver)//loop to ensure it isnt awoken at the wrong time
				{
					onOahu.sleep();
				}
				driver = true;
				bg.AdultRowToMolokai();
				adultsOnOahu--;
				boat = Molokai;
				loc = Molokai;
				driver = false;
				totalOnMolokai++;//no need to report this, as an adult should never be the last one returning
				onMolokai.wakeAll();
				onMolokai.sleep();

			}
			else//adult on molokai
			{
				onMolokai.sleep();//if an adult is on molokai, all it should ever do is sleep
			}
		}
		boatLock.release();

    }

    static void ChildItinerary()
    {
//	bg.initializeChild(); //Required for autograder interface. Must be the first thing called.
	//DO NOT PUT ANYTHING ABOVE THIS LINE. 
		boolean loc = Oahu;//local variable for storing location of each individual
		boatLock.acquire();
		while(!finished)
		{
			if(loc == Oahu)
			{
				while(boat!=Oahu || rider || (childrenOnOahu == 1 && adultsOnOahu > 0))//loop to ensure it isnt awoken at the wrong time
				{
					onOahu.sleep();
				}
				onOahu.wakeAll();
				if(childrenOnOahu>1||(childrenOnOahu==1 && !rider && driver)) //send all the children first
				{
					if(driver)
						rider = true;
					else
						driver = true;
					if(rider&&driver)
					{
						onBoat.wake();//tells the driver to drive
						onBoat.sleep();//waits for the driver to drive

						//once woken ride to molokai
						bg.ChildRideToMolokai();
						childrenOnOahu--;
						rider = false;
						driver = false;
						boat = Molokai;
						loc = Molokai;
						endCon.speak(++totalOnMolokai);//increment the total on the island, and report it

						onMolokai.wakeAll();//wakes a child in Molokai

						onMolokai.sleep();//put this child to sleep


					}
					else if(driver)
					{
						onBoat.sleep();
						bg.ChildRowToMolokai();
						childrenOnOahu--;
						driver = false;//these next few lines will end up being redundant, however they are needed for the edge case
						boat = Molokai;
						loc = Molokai;
						endCon.speak(++totalOnMolokai);//increment the total on the island, and report it
						onBoat.wake();//wakes up the passengers
						onMolokai.sleep();//sleeps on the island, will be woken by the passenger to drive back
					}else
					{
						System.out.println("????????????NO DRIVER???????????????????????????????????????????????");
					}

				}
				else if(childrenOnOahu==1&&adultsOnOahu==0) //the case when all the adults are across and there is only 1 child left. This should only happen when there is an odd number of kids/no adults
				{
						bg.ChildRowToMolokai();
						childrenOnOahu--;
						boat = Molokai;
						loc = Molokai;
						endCon.speak(++totalOnMolokai);//increment the total on the island, and report it
						onMolokai.sleep();//probably not needed, as the end should already be triggered
				}

			}
			else //Child on Molokai
			{
				//this means there is at least 1 child on Molokai if there somehow isnt this will err(generate a new child?): that should however be impossible. If it does we can split the total into adults and kids, and make sure there is a kid on molokai
				while(boat!=Molokai)
				{
					onMolokai.sleep();
				}
				driver = true;
				bg.ChildRowToOahu();
				totalOnMolokai--;
				boat = Oahu;
				loc = Oahu;
				driver = false;
				childrenOnOahu++;
				onOahu.wakeAll();
				onOahu.sleep();
			}
		}
		boatLock.release();

    }
    //deleted unused function
}
