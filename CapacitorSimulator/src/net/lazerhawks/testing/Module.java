package net.lazerhawks.testing;

public class Module implements Comparable<Module>{
	
	private String name;
	private Double cycleTime;
	private Double reloadTime;
	private Integer maxCharges;
	private Integer currentCharges;
	private Boolean localEffect;
	private Double capacitorUsage;
	private Double nextCycleTime;
	
	private Integer attempts;
	private Integer failures;
	
	public Module()
	{	
		//0 = constant
		this.reloadTime = 0.00d;
		
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

	public Double getCycleTime() {
		return cycleTime;
	}

	public void setCycleTime(Double cycleTime) {
		this.cycleTime = cycleTime;
		//this.nextCycleTime = cycleTime;	//Should actually do an initial pass on all modules before first queue sort
	}

	public Double getReloadTime() {
		return reloadTime;
	}

	public void setReloadTime(Double reloadTime) {
		this.reloadTime = reloadTime;
	}

	public Boolean getLocalEffect() {
		return localEffect;
	}

	public void setLocalEffect(Boolean localEffect) {
		this.localEffect = localEffect;
	}

	public Double getCapacitorUsage() {
		return capacitorUsage;
	}

	public void setCapacitorUsage(Double capacitorUsage) {
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


	public Double getNextCycleTime() {
		return nextCycleTime;
	}

	public void setNextCycleTime(Double nextCycleTime) {
		this.nextCycleTime = nextCycleTime;
	}
	
	public int compareTo(Module compareModule) {
		
		if(compareModule.getNextCycleTime() < this.getNextCycleTime())
		{
			return 1;
		}
		else if(compareModule.getNextCycleTime() > this.getNextCycleTime())
		{
			return -1;
		}
		else
		{
			return 0;
		}
		
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
