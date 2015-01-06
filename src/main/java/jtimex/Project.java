package jtimex;

import java.time.Duration;
import java.time.LocalTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.beans.property.SimpleStringProperty;

public class Project 
{
	
    private final SimpleStringProperty name;
    private final SimpleStringProperty duration;
    private LocalTime startTime;
    private Duration currentDuration = Duration.ZERO;
    private long projectId;
 
    public Project(String name) {
    	this(name, "00:00:00", null);
    }

    public Project(String name, String duration)
    {
    	this(name, duration, null);
    }

    public Project(String name, String duration, LocalTime startTime)
    {
        this.name = new SimpleStringProperty(name);
        this.duration = new SimpleStringProperty(duration);
        this.startTime = startTime;
    }
    
    public SimpleStringProperty nameProperty()
    {
    	return name;
    }
    
    public SimpleStringProperty durationProperty()
    {
    	return duration;
    }

	public String getName() {
		return name.get();
	}
 
	public void setName(String nname) {
		name.set(nname);
	}

	public String getDuration() {
		return duration.get();
	}
 
	public void setDuration(String nduration) {
		duration.set(nduration);
	}
	
	public void setDuration(Duration d)
	{
		setDuration(getDurationAsString(d));
	}
	
	public void tick()
	{
		if (startTime != null)
		{
			Duration d = Duration.between(startTime, LocalTime.now()).plus(currentDuration);
			setDuration(d);
		}
	}
	
	public Duration getCurrentDuration()
	{
		if (isActive())
		{
			return Duration.between(startTime, LocalTime.now()).plus(currentDuration);
		}
		else
		{
			return currentDuration;
		}
	}
	
	public void editDuration(String s)
	{
		Pattern p = Pattern.compile("(\\d\\d):(\\d\\d):(\\d\\d)");
		Matcher m = p.matcher(s);
		if (!m.matches())
		{
			throw new IllegalArgumentException("Invalid time");
		}
		int hours = Integer.parseInt(m.group(1));
		int minutes = Integer.parseInt(m.group(2));
		int seconds = Integer.parseInt(m.group(3));
		Duration duration = Duration.ofSeconds(seconds)
				.plus(Duration.ofMinutes(minutes))
				.plus(Duration.ofHours(hours));
		setCurrentDuration(duration);
	}
	
	public Project activate()
	{
		if (!isActive())
		{
			startTime = LocalTime.now();
		}
		System.out.println("Activated project " + getName());
		return this;
	}

	public void deactivate() 
	{
		if (startTime != null)
		{
			currentDuration = Duration.between(startTime, LocalTime.now()).plus(currentDuration);
			startTime = null;
		}
		System.out.println("Deactivated project " + getName());
	}
	
	public void endOfDay() 
	{
		if (startTime != null)
		{
			currentDuration = Duration.between(startTime, LocalTime.MAX);
			startTime = null;
		}
	}
	
	public void setCurrentDuration(Duration duration)
	{
		currentDuration = duration;
		setDuration(duration);
	}
	
	public void setStartTime(LocalTime startTime)
	{
		this.startTime = startTime;
	}

	public boolean isActive()
	{
		return (startTime != null);
	}

	public LocalTime getStartTime() 
	{
		return startTime;
	}

	public void setId(long id) 
	{
		projectId = id;
	}
	
	public long getId()
	{
		return projectId;
	}
	
	public static String getDurationAsString(Duration d)
	{
		long hours = d.toHours();
		long minutes = d.minusHours(hours).toMinutes();
		long seconds = d.minusHours(hours).minusMinutes(minutes).getSeconds();

		String nduration = String.format("%02d:%02d:%02d", hours, minutes, seconds);
		return nduration;
	}

}
