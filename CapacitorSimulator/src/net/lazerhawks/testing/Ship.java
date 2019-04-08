package net.lazerhawks.testing;

public class Ship {
	
	private String name;
	
	//Will also be used to calculate cap passive regen
	private Double capacitorMax;
	private Double capacitorCurrent;
	private Double capacitorRechargeTime;
	
	public Ship(String _name, Double _capacitorMax, Double _capacitorRechargeTime)
	{
		this.name = _name;
		this.capacitorMax = _capacitorMax;
		this.capacitorCurrent = capacitorMax; //Set initial cap to what maximum is
		this.capacitorRechargeTime = _capacitorRechargeTime;
	}
	
	public void changeCapacitorLevel(Double _value)
	{
		capacitorCurrent = capacitorCurrent + _value;
		
		if(capacitorCurrent > capacitorMax)
		{
			capacitorCurrent = capacitorMax;
		}
		else if(capacitorCurrent < 0)
		{
			capacitorCurrent = 0.00d;
		}
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Double getCapacitorMax() {
		return capacitorMax;
	}

	public void setCapacitorMax(Double capacitorMax) {
		this.capacitorMax = capacitorMax;
	}

	public Double getCapacitorCurrent() {
		return capacitorCurrent;
	}

	public void setCapacitorCurrent(Double capacitorCurrent) {
		this.capacitorCurrent = capacitorCurrent;
	}

	public Double getCapacitorRechargeTime() {
		return capacitorRechargeTime;
	}

	public void setCapacitorRechargeTime(Double capacitorRechargeTime) {
		this.capacitorRechargeTime = capacitorRechargeTime;
	}

}
