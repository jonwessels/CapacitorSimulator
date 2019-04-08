package net.lazerhawks.testing;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Module implements Comparable<Module>{
	
	private String name;
	private BigDecimal cycleTime;
	private BigDecimal reloadTime;
	private Integer maxCharges;
	private Integer currentCharges;
	private Boolean localEffect;
	private BigDecimal capacitorUsage;
	private BigDecimal nextCycleTime;
	
	private Integer attempts;
	private Integer failures;
	
	public Module()
	{	
		//0 = constant
		this.reloadTime = new BigDecimal(0).setScale(4, RoundingMode.HALF_DOWN);
		
		//0 = doesn't use charges
		this.maxCharges = 0;
		this.currentCharges = 0;
	}
	
	public void consumeCharge()
	{
		currentCharges--;
	}
	
	public void refillCharges()
	{
		currentCharges = maxCharges;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public BigDecimal getCycleTime() {
		return cycleTime;
	}

	public void setCycleTime(BigDecimal cycleTime) {
		this.cycleTime = cycleTime;
		//this.nextCycleTime = cycleTime;	//Should actually do an initial pass on all modules before first queue sort
	}

	public BigDecimal getReloadTime() {
		return reloadTime;
	}

	public void setReloadTime(BigDecimal reloadTime) {
		this.reloadTime = reloadTime;
	}

	public Boolean getLocalEffect() {
		return localEffect;
	}

	public void setLocalEffect(Boolean localEffect) {
		this.localEffect = localEffect;
	}

	public BigDecimal getCapacitorUsage() {
		return capacitorUsage;
	}

	public void setCapacitorUsage(BigDecimal capacitorUsage) {
		this.capacitorUsage = capacitorUsage;
	}

	public Integer getMaxCharges() {
		return maxCharges;
	}

	public void setMaxCharges(Integer maxCharges) {
		this.maxCharges = maxCharges;
	}

	public Integer getCurrentCharges() {
		return currentCharges;
	}

	public void setCurrentCharges(Integer currentCharges) {
		this.currentCharges = currentCharges;
	}


	public BigDecimal getNextCycleTime() {
		return nextCycleTime;
	}

	public void setNextCycleTime(BigDecimal nextCycleTime) {
		this.nextCycleTime = nextCycleTime;
	}
	
	public int compareTo(Module compareModule) {
		
		return this.nextCycleTime.compareTo(compareModule.nextCycleTime);
		
	}

	public Integer getAttempts() {
		return attempts;
	}

	public void setAttempts(Integer attempts) {
		this.attempts = attempts;
	}

	public Integer getFailures() {
		return failures;
	}

	public void setFailures(Integer failures) {
		this.failures = failures;
	}	

}
