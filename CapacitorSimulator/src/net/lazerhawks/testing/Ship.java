package net.lazerhawks.testing;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Ship {
	
	private String name;
	
	//Will also be used to calculate cap passive regen
	private BigDecimal capacitorMax;
	private BigDecimal capacitorCurrent;
	private BigDecimal capacitorRechargeTime;
	
	public Ship(String _name, BigDecimal _capacitorMax, BigDecimal _capacitorRechargeTime)
	{
		this.name = _name;
		this.capacitorMax = _capacitorMax;
		this.capacitorCurrent = capacitorMax; //Set initial cap to what maximum is
		this.capacitorRechargeTime = _capacitorRechargeTime;
	}
	
	public void changeCapacitorLevel(BigDecimal _value)
	{
		capacitorCurrent = capacitorCurrent.add(_value).setScale(4, RoundingMode.HALF_DOWN);
		
		if(capacitorCurrent.compareTo(capacitorMax) > 0)
		{
			capacitorCurrent = capacitorMax;
		}
		else if(capacitorCurrent.compareTo(BigDecimal.ZERO) < 0)
		{
			capacitorCurrent = new BigDecimal(0.00).setScale(4, RoundingMode.HALF_DOWN);
		}
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public BigDecimal getCapacitorMax() {
		return capacitorMax;
	}

	public void setCapacitorMax(BigDecimal capacitorMax) {
		this.capacitorMax = capacitorMax;
	}

	public BigDecimal getCapacitorCurrent() {
		return capacitorCurrent;
	}

	public void setCapacitorCurrent(BigDecimal capacitorCurrent) {
		this.capacitorCurrent = capacitorCurrent;
	}

	public BigDecimal getCapacitorRechargeTime() {
		return capacitorRechargeTime;
	}

	public void setCapacitorRechargeTime(BigDecimal capacitorRechargeTime) {
		this.capacitorRechargeTime = capacitorRechargeTime;
	}

}
