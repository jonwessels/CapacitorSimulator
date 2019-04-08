package net.lazerhawks.testing;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
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


	static BigDecimal retryTime = new BigDecimal(0.1);

	MathContext mc = new MathContext(12,RoundingMode.HALF_UP);


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

			//BigDecimal downtime = new BigDecimal(retryTime * consumer.getFailures());
			BigDecimal totalUptime = new BigDecimal((consumer.getAttempts()-consumer.getFailures()), mc).multiply(consumer.getCycleTime(), mc).setScale(2, RoundingMode.HALF_DOWN);

			System.out.println("Total Uptime(s): " + totalUptime.toString());

			BigDecimal percentUptime = totalUptime.divide(new BigDecimal(maxTicks), mc).multiply(new BigDecimal(100), mc).setScale(2, RoundingMode.HALF_DOWN);

			System.out.println("Uptime(%): " + percentUptime.toString());

			System.out.println("---------------------");

		}

		//Check generator usage
		while(localGenerateQueue.peek() != null)
		{
			
			//Attempts = normal
			//Failures = reload
			
			Module generator = localGenerateQueue.poll();
			System.out.println("---------------------");
			System.out.println("Module: " + generator.getName());
			System.out.println("Activations: " + generator.getAttempts());
			System.out.println("Reloads: " + generator.getFailures());

														//We add the reloads as well, cause the module had to do a normal cycle before reloading
			BigDecimal totalNormalTime = new BigDecimal(generator.getAttempts()).multiply(generator.getCycleTime());
			BigDecimal totalReloadTime = new BigDecimal(generator.getFailures()).multiply(generator.getReloadTime());
			BigDecimal totalIdleTime = new BigDecimal(maxTicks).subtract(totalNormalTime.add(totalReloadTime));

			System.out.println("Total Activation Time(s): " + totalNormalTime.toString());
			System.out.println("Total Reload Time(s): " + totalReloadTime.toString());
			System.out.println("Total Idle Time(s): " + totalIdleTime.toString());

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
		while(localConsumeQueue.peek().getNextCycleTime().compareTo(new BigDecimal(serverTick)) <= 0)
		{

			Module peek = localConsumeQueue.peek();

			System.out.println("Trying module: " + peek.getName());
			System.out.println("Cap needed: " + peek.getCapacitorUsage());
			System.out.println("Cap available: " + targetShip.getCapacitorCurrent());

			//Check if there is enough capacitor
			if(localConsumeQueue.peek().getCapacitorUsage().add(targetShip.getCapacitorCurrent()).compareTo(BigDecimal.ZERO) >= 0)
			{
				System.out.println("Enough cap");

				//Remove module from front of queue
				Module moduleConsume = localConsumeQueue.poll();
				//Update it's next cycle time
				moduleConsume.setNextCycleTime(moduleConsume.getNextCycleTime().add(moduleConsume.getCycleTime()));
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
				if(!localGenerateQueue.isEmpty() && localGenerateQueue.peek().getNextCycleTime().compareTo(localConsumeQueue.peek().getNextCycleTime()) <= 0)
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
						
						moduleGenerate.setAttempts(moduleGenerate.getAttempts() + 1);

						//Use normal cycle time if still charges
						if(moduleGenerate.getCurrentCharges() > 0)
						{
							System.out.println("Cycling normally");
							moduleGenerate.setNextCycleTime(moduleGenerate.getNextCycleTime().add(moduleGenerate.getCycleTime()));
						}
						//Otherwise use reload time
						else if(moduleGenerate.getCurrentCharges() == 0)
						{
							System.out.println("Reloading");										//Have to finish cycle before you can reload
							moduleGenerate.setNextCycleTime(moduleGenerate.getNextCycleTime().add(moduleGenerate.getCycleTime()).add(moduleGenerate.getReloadTime()));
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
						moduleGenerate.setNextCycleTime(moduleGenerate.getNextCycleTime().add(moduleGenerate.getCycleTime()));
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
					if(localConsumeQueue.peek().getCapacitorUsage().add(targetShip.getCapacitorCurrent()).compareTo(BigDecimal.ZERO) >= 0)
					{
						//Remove module from front of queue
						Module moduleConsume = localConsumeQueue.poll();
						//Update it's next cycle time
						moduleConsume.setNextCycleTime(moduleConsume.getNextCycleTime().add(moduleConsume.getCycleTime()));
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
						moduleConsume.setNextCycleTime(moduleConsume.getNextCycleTime().add(retryTime));
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
					moduleConsume.setNextCycleTime(moduleConsume.getNextCycleTime().add(retryTime));
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

		BigDecimal capacitorChange = new BigDecimal(0);

		//Check if head of queue can happen within this tick
		while(remoteQueue.peek().getNextCycleTime().compareTo(new BigDecimal(serverTick)) <= 0)
		{

			Module peek = remoteQueue.peek();

			System.out.println("Trying module: " + peek.getName());
			System.out.println("Cap change: " + peek.getCapacitorUsage());

			//Remove module from front of queue
			Module moduleRemote = remoteQueue.poll();
			//Update it's next cycle time
			moduleRemote.setNextCycleTime(moduleRemote.getNextCycleTime().add(moduleRemote.getCycleTime()));
			//Add the module back into the queue
			remoteQueue.add(moduleRemote);
			//alter capacitorChange

			BigDecimal currentChange = moduleRemote.getCapacitorUsage().subtract(moduleRemote.getCapacitorUsage().multiply(targetShip.getNeutResistance(), mc)).setScale(4, RoundingMode.HALF_DOWN);

			capacitorChange = capacitorChange.add(currentChange);


			System.out.println("Current cap change: " + capacitorChange.toString());
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
				if(module.getCapacitorUsage().compareTo(BigDecimal.ZERO) > 0)
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


				BigDecimal cycleSpread = new BigDecimal(rand.nextInt(module.getCycleTime().intValue()));

				module.setNextCycleTime(module.getCycleTime().add(cycleSpread));

				remoteQueue.add(module);
			}
		}

		currentTick = 0;

		targetShip.setCapacitorCurrent(targetShip.getCapacitorMax());

	}


	private BigDecimal currentPassiveRegen()
	{
		//10*MaxCap/Time * (sqrt(CurCap/MaxCap) - (CurCap/MaxCap))

		BigDecimal currentPercent = targetShip.getCapacitorCurrent().divide(targetShip.getCapacitorMax(), mc).setScale(4, RoundingMode.HALF_DOWN);
		BigDecimal pairs = currentPercent.sqrt(mc).subtract(currentPercent).setScale(4, RoundingMode.HALF_DOWN);
		BigDecimal capRegen = new BigDecimal(10).multiply(targetShip.getCapacitorMax()).divide(targetShip.getCapacitorRechargeTime(), mc).setScale(4, RoundingMode.HALF_DOWN);
		capRegen = capRegen.multiply(pairs);

		//Use all decimals for calculations and return the rounded value to 4 decimals. Assume round down to get 'worst' case
		return capRegen.setScale(4, RoundingMode.HALF_DOWN);

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
