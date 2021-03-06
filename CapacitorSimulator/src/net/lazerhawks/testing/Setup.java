package net.lazerhawks.testing;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class Setup {

	public static void main( String[ ] args ) 
	{

		System.out.println("Setup");

		String directory = System.getProperty("user.dir");

		System.out.println("Directory: " + directory);
		


		String csvModules = directory + "/modules.csv";
		String csvTarget = directory + "/target_ship.csv";
		List<Module> moduleList = new ArrayList<Module>();
		Ship targetShip = null;
		BufferedReader br = null;
		String line = "";
		String cvsSplitBy = ",";

		try 
		{

			br = new BufferedReader(new FileReader(csvModules));
			while ((line = br.readLine()) != null) 
			{
				// use comma as separator
				String[] module = line.split(cvsSplitBy);

				// 0	1		2			3			4			5		6
				//name,quantity,cycle_time,reload_time,max_charges,local,cap_usage

				//Skip header
				if(!module[0].equals("name")) 
				{
					System.out.println("Module: " + module[0]);
					Integer quantity = Integer.valueOf(module[1]);
					System.out.println("Quantity: " + quantity);
					
					for(int i = 0; i < quantity; i++)
					{
						Module csvModule = new Module();
						csvModule.setName(module[0]);
						csvModule.setCycleTime(new BigDecimal(module[2]));
						csvModule.setReloadTime(new BigDecimal(module[3]));
						csvModule.setMaxCharges(Integer.valueOf(module[4]));
						csvModule.setLocalEffect(Boolean.parseBoolean(module[5]));
						csvModule.setCapacitorUsage(new BigDecimal(module[6]));
						
						moduleList.add(csvModule);
					}
				}

			}
			
			br = new BufferedReader(new FileReader(csvTarget));
			while ((line = br.readLine()) != null) 
			{
				// use comma as separator
				String[] ship = line.split(cvsSplitBy);

				// 0	1		2	
				//name,max_cap,recharge_time

				//Skip header
				if(!ship[0].equals("name")) 
				{
					System.out.println("Ship: " + ship[0]);
	
					targetShip = new Ship(ship[0], new BigDecimal(ship[1]), new BigDecimal(ship[2]), new BigDecimal(ship[3]));		
				}

			}

			Simulation simulation = new Simulation(3600, targetShip, moduleList);

		} 
		catch (FileNotFoundException e) 
		{
			e.printStackTrace();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		} 
		finally 
		{
			if (br != null) 
			{
				try 
				{
					br.close();
				} 
				catch (IOException e) 
				{
					e.printStackTrace();
				}
			}
		}

	}

}
