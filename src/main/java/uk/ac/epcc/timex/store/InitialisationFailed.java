package uk.ac.epcc.timex.store;

public class InitialisationFailed extends Error
{

	public InitialisationFailed() 
	{
		super("Failed to initialise data store.");
	}
	
}
