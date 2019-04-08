package net.lazerhawks.testing;

import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;

public class Simulation {

	private List<Module> moduleList; //Keep original list of modules stored to allow for reset
	//private Module passiveRegen;	Don't need to add in queue. Just start each tick by checking regen instead?
	private Ship targetShip;

	private Integer currentTick;	//Current server time. Uses Double in case finer tracking for local modules is needed later, but ticks only happen on round numbers
	private Integer maxTicks;	//How long simulation should run for. 1 tick = 1 second

	private PriorityQueue<Module> localConsumeQueue = new PriorityQueue<Module>(); //Will constantly try to consume
	private PriorityQueue<Module> localGenerateQueue = new PriorityQueue<Module>(); //Will generate as needed
	private PriorityQueue<Module> remoteQueue = new PriorityQueue<Module>(); //Will always happen, regardless of local status


	static double retryTime = 0.1;


	public Simulation(Integer _maxTicks, Ship _targetShip, List<Module> _moduleList)
	{
		this.maxTicks = _maxTicks;
		this.targetShip = _targetShip;
		this.moduleList = _moduleList;

		reset();

		System.out.println("Testing ship: " + targetShip.getName());
		printShipStatus();

		System.out.println("Module count: " + moduleList.size());
		System.out.println("Local Consume count: " + localConsumeQueue.size());
		System.out.println("Local Generate count: " + localGenerateQueue.size());
		System.out.println("Remote count: " + remoteQueue.size());


		System.out.println("Max Ticks: " + maxTicks);

		tickServer();
		moduleSummary();

	}
	
	private void moduleSummary()
	{
		//Check consumer uptime
		while(localConsumeQueue.peek() != null)
		{
			Module consumer = localConsumeQueue.poll();
			System.out.println("---------------------");
			System.out.println("Module: " + consumer.getName());
			System.out.println("Attempts: " + consumer.getAttempts());
			System.out.println("Failures: " + consumer.getFailures());
			
			Double downtime = retryTime * consumer.getFailures();
			Double totalUptime = (consumer.getAttempts()-consumer.getFailures()) * consumer.getCycleTime();
			
			System.out.println("Total Uptime(s): " + (double) Math.round(totalUptime));
			
			Double percentUptime = (totalUptime/maxTicks) * 100;
			
			System.out.println("Uptime(%): " + (double) Math.round(percentUptime));
			
			System.out.println("---------------------");
			
		}
	}

	private void tickServer()
	{
		currentTick++;
		System.out.println("Current tick: " + currentTick);

		//Add the current passive regen to the ship first
		System.out.println("Adding regen: " + currentPassiveRegen());
		targetShip.changeCapacitorLevel(currentPassiveRegen());

		printShipStatus();

		//First process local
		processLocalQueue(currentTick);
		
		//Then process remotes
		processRemoteQueue(currentTick);


		if(currentTick < maxTicks)
		{
			tickServer();
		}
	}

	//Local queues happen as long as they are within the current tick. They are not grouped up and processed together like Remotes
	private void processLocalQueue(Integer serverTick)
	{

		System.out.println("Running local modules");

		//Check if head of queue can happen within this tick
		while(localConsumeQueue.peek().getNextCycleTime() <= serverTick)
		{

			Module peek = localConsumeQueue.peek();

			System.out.println("Trying module: " + peek.getName());
			System.out.println("Cap needed: " + peek.getCapacitorUsage());
			System.out.println("Cap available: " + targetShip.getCapacitorCurrent());

			//Check if there is enough capacitor
			if(localConsumeQueue.peek().getCapacitorUsage() + targetShip.getCapacitorCurrent() >= 0)
			{
				System.out.println("Enough cap");

				//Remove module from front of queue
				Module moduleConsume = localConsumeQueue.poll();
				//Update it's next cycle time
				moduleConsume.setNextCycleTime(moduleConsume.getNextCycleTime()+moduleConsume.getCycleTime());
				moduleConsume.setAttempts(moduleConsume.getAttempts() + 1);
				//Add the module back into the queue
				localConsumeQueue.add(moduleConsume);
				//Change ship capacitor
				targetShip.changeCapacitorLevel(moduleConsume.getCapacitorUsage());

				System.out.println("Cap after use: " + targetShip.getCapacitorCurrent());
			}
			else
			{
				System.out.println("Not enough cap");

				//Check if there are any generators available at the time of the current consumer
				//Make sure generator is able to activate at the time the consumer is activating
				if(!localGenerateQueue.isEmpty() && localGenerateQueue.peek().getNextCycleTime() <= localConsumeQueue.peek().getNextCycleTime())
				{
					//Remove module from front of queue
					Module moduleGenerate = localGenerateQueue.poll();

					System.out.println("Generator available: " + moduleGenerate.getName());

					//Check if uses charges
					if(moduleGenerate.getMaxCharges() > 0)
					{
						System.out.println("Uses charges");

						System.out.println("Charges start: " + moduleGenerate.getCurrentCharges());

						moduleGenerate.consumeCharge();

						System.out.println("Charges left: " + moduleGenerate.getCurrentCharges());

						//Use normal cycle time if still charges
						if(moduleGenerate.getCurrentCharges() > 0)
						{
							System.out.println("Cycling normally");
							moduleGenerate.setNextCycleTime(moduleGenerate.getNextCycleTime()+moduleGenerate.getCycleTime());
							moduleGenerate.setAttempts(moduleGenerate.getAttempts() + 1);
						}
						//Otherwise use reload time
						else if(moduleGenerate.getCurrentCharges() == 0)
						{
							System.out.println("Reloading");
							moduleGenerate.setNextCycleTime(moduleGenerate.getNextCycleTime()+moduleGenerate.getReloadTime());
							//Counting reload as failures purely to be able to record the different time. Is not an actual 'failure'
							moduleGenerate.setFailures(moduleGenerate.getFailures() + 1);

							//Refill charges
							moduleGenerate.refillCharges();

						}
					}
					//Otherwise use normal cycle time
					else
					{
						System.out.println("Doesn't use charges");
						moduleGenerate.setNextCycleTime(moduleGenerate.getNextCycleTime()+moduleGenerate.getCycleTime());
						moduleGenerate.setAttempts(moduleGenerate.getAttempts() + 1);
					}

					//Add the module back into the queue
					localGenerateQueue.add(moduleGenerate);;

					//Change ship capacitor
					targetShip.changeCapacitorLevel(moduleGenerate.getCapacitorUsage());

					System.out.println("Cap after use: " + targetShip.getCapacitorCurrent());


					System.out.println("Trying module: " + peek.getName());
					System.out.println("Cap needed: " + peek.getCapacitorUsage());
					System.out.println("Cap available: " + targetShip.getCapacitorCurrent());

					//Check if there is enough capacitor
					if(localConsumeQueue.peek().getCapacitorUsage() + targetShip.getCapacitorCurrent() >= 0)
					{
						//Remove module from front of queue
						Module moduleConsume = localConsumeQueue.poll();
						//Update it's next cycle time
						moduleConsume.setNextCycleTime(moduleConsume.getNextCycleTime()+moduleConsume.getCycleTime());
						moduleConsume.setAttempts(moduleConsume.getAttempts() + 1);
						//Add the module back into the queue
						localConsumeQueue.add(moduleConsume);
						//Change ship capacitor
						targetShip.changeCapacitorLevel(moduleConsume.getCapacitorUsage());

						System.out.println("Cap after use: " + targetShip.getCapacitorCurrent());
					}
					else
					{
						System.out.println("Failed to generate enough cap");

						//No generators left this tick
						//Remove module from front of queue
						Module moduleConsume = localConsumeQueue.poll();
						//Update it's next cycle time slightly, allowing other modules a chance to activate
						moduleConsume.setNextCycleTime(moduleConsume.getNextCycleTime() + retryTime);
						moduleConsume.setAttempts(moduleConsume.getAttempts() + 1);
						moduleConsume.setFailures(moduleConsume.getFailures() + 1);
						//Add the module back into the queue
						localConsumeQueue.add(moduleConsume);
					}
				}
				else
				{
					System.out.println("Failed to generate enough cap");

					//No generators left this tick
					//Remove module from front of queue
					Module moduleConsume = localConsumeQueue.poll();
					//Update it's next cycle time slightly, allowing other modules a chance to activate
					moduleConsume.setNextCycleTime(moduleConsume.getNextCycleTime() + retryTime);
					moduleConsume.setAttempts(moduleConsume.getAttempts() + 1);
					moduleConsume.setFailures(moduleConsume.getFailures() + 1);
					//Add the module back into the queue
					localConsumeQueue.add(moduleConsume);
				}
			}
		}
	}

	//Remote queue is slightly different, in that everything is added together as one big effect and then processed onto the ship
	private void processRemoteQueue(Integer serverTick)
	{

		System.out.println("Running remote modules");

		Double capacitorChange = 0.00d;

		//Check if head of queue can happen within this tick
		while(remoteQueue.peek().getNextCycleTime() <= serverTick)
		{

			Module peek = remoteQueue.peek();

			System.out.println("Trying module: " + peek.getName());
			System.out.println("Cap change: " + peek.getCapacitorUsage());

			//Remove module from front of queue
			Module moduleRemote = remoteQueue.poll();
			//Update it's next cycle time
			moduleRemote.setNextCycleTime(moduleRemote.getNextCycleTime()+moduleRemote.getCycleTime());
			//Add the module back into the queue
			remoteQueue.add(moduleRemote);
			//alter capacitorChange
			capacitorChange = capacitorChange + moduleRemote.getCapacitorUsage();
			
			
			System.out.println("Current cap change: " + capacitorChange);
		}
		
		//Change ship capacitor
		targetShip.changeCapacitorLevel(capacitorChange);
	}

	private void reset()
	{
		System.out.println("Resetting");

		localConsumeQueue.clear();
		localGenerateQueue.clear();
		remoteQueue.clear();
		
		Random rand = new Random();

		for(Module module :moduleList)
		{
			module.setNextCycleTime(module.getCycleTime());
			module.refillCharges();
			module.setAttempts(0);
			module.setFailures(0);

			//Check if module is part of targetShip
			if(module.getLocalEffect())
			{
				//Does it generate cap, or use it
				if(module.getCapacitorUsage() > 0)
				{
					localGenerateQueue.add(module);
				}
				else
				{
					localConsumeQueue.add(module);
				}
			}
			else
			{
				//Trying to spread out modules at start
				//Might need to implement proper stagger method
				//Otherwise, can run multiple simulations with same data, and generate averages
				module.setNextCycleTime(module.getCycleTime() + rand.nextInt(module.getCycleTime().intValue()));
				
				remoteQueue.add(module);
			}
		}

		currentTick = 0;

		targetShip.setCapacitorCurrent(targetShip.getCapacitorMax());

	}


	private Double currentPassiveRegen()
	{
		//10*MaxCap/Time * (sqrt(CurCap/MaxCap) - (CurCap/MaxCap))

		Double currentPercent = targetShip.getCapacitorCurrent() / targetShip.getCapacitorMax();
		Double pairs = (Math.sqrt(currentPercent)) - currentPercent;
		Double capRegen = ((10 * targetShip.getCapacitorMax()) / targetShip.getCapacitorRechargeTime()) * pairs;

		return (double) Math.round(capRegen);

	}

	private void printShipStatus()
	{
		System.out.println("***Target ship status***");
		System.out.println("Max Capacitor: " + targetShip.getCapacitorMax());
		System.out.println("Recharge Time: " + targetShip.getCapacitorRechargeTime());
		System.out.println("Current Capacitor: " + targetShip.getCapacitorCurrent());
		System.out.println("Current Cap/s: " + currentPassiveRegen());
		System.out.println("************************");
	}

}
