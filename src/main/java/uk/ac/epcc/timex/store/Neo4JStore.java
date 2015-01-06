package uk.ac.epcc.timex.store;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.neo4j.graphdb.ConstraintViolationException;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.schema.IndexDefinition;
import org.neo4j.graphdb.schema.Schema;
import org.neo4j.tooling.GlobalGraphOperations;

import uk.ac.epcc.timex.Project;

public class Neo4JStore implements DataStore
{
    private String dbPath = "target/neo4j-timex-db";

    GraphDatabaseService graphDb;
	private final Label projectLabel = DynamicLabel.label( "Project" );
	private final Label dateLabel = DynamicLabel.label( "TimexRecord" );

    public Neo4JStore() 
    {
	}

    public Neo4JStore(String dbPath) 
    {
    	this.dbPath = dbPath;
	}

    @Override
	public void init()
    {
    	try
    	{
    		graphDb = new GraphDatabaseFactory().newEmbeddedDatabase( dbPath );
    	}
    	catch (Throwable e)
    	{
    		InitialisationFailed exc = new InitialisationFailed();
    		exc.initCause(e);
    		throw exc;
    	}
    	registerShutdownHook( graphDb );
    	
    	IndexDefinition indexDefinition;
    	try ( Transaction tx = graphDb.beginTx() )
    	{
    	    Schema schema = graphDb.schema();
    	    indexDefinition = schema.indexFor( DynamicLabel.label( "TimexRecord" ) )
    	            .on( "date" )
    	            .create();
    	    tx.success();
    	}
    	catch (ConstraintViolationException e)
    	{
    		System.err.println("Timex date index already exists.");
    	}

    	System.out.println("Created index");
    }
    
    @Override
    public void storeTimexData(LocalDate date, List<Project> projects, Project activeProject)
    {
        try ( Transaction tx = graphDb.beginTx() )
        {
        	Node dateNode = null;
    	    try ( ResourceIterator<Node> dateNodes =
    	    		graphDb.findNodesByLabelAndProperty(dateLabel, "date", date.toString()).iterator() )
    	    {
    	    	while (dateNodes.hasNext())
    	    	{
    	    		dateNode = dateNodes.next();
//    	    		System.out.println("Found existing node for date " + date);
    	    	}
    	    }
    	    if (dateNode == null)
    	    {
    	    	dateNode = graphDb.createNode();
            	dateNode.addLabel(dateLabel);
//	    		System.out.println("Created new node for date " + date);
    	    }
        	for (String key : dateNode.getPropertyKeys())
        	{
        		dateNode.removeProperty(key);
        	}
        	dateNode.setProperty( "date", date.toString());
        	for (Project project : projects)
        	{
        		System.out.println("Storing project " + project.getName() + ", duration: " + project.getCurrentDuration());

        		dateNode.setProperty(
        				"#" + project.getName(), 
        				project.getCurrentDuration().toString());
        	}
        	if (activeProject != null)
        	{
        		dateNode.setProperty("active", activeProject.getName());
        		dateNode.setProperty("startTime", activeProject.getStartTime().toString());
        		System.out.println("Active project: " + activeProject.getName());
        		System.out.println("Start time: " + activeProject.getStartTime());
        		System.out.println("Current duration: " + activeProject.getCurrentDuration());
        	}
        	
            tx.success();
            System.out.println("Stored data node for date " + date.toString());
        }

    }
    
    @Override
    public Project loadTimexData(LocalDate date, List<Project> projects, boolean isToday)
    {
    	Map<String, Project> projectsByName = new HashMap<>();
    	projects.forEach(p -> projectsByName.put(p.getName(), p));
    	Project activeProject = null;
    	
        try ( Transaction tx = graphDb.beginTx() )
        {
    	    try ( ResourceIterator<Node> dateNodes =
    	    		graphDb.findNodesByLabelAndProperty(dateLabel, "date", date.toString()).iterator() )
    	    {
    	    	while (dateNodes.hasNext())
    	    	{
	    			System.out.println("Loading timex data");
    	    		Node node = dateNodes.next();
    	    		String activeName = null;
    	    		LocalTime startTime = null;
    	    		if (node.hasProperty("active"))
    	    		{
    	    			activeName = (String)node.getProperty("active");
    	    		}
    	    		if (node.hasProperty("startTime"))
    	    		{
    	    			startTime = LocalTime.parse((String)node.getProperty("startTime"));
    	    		}

    	    		for (String key : node.getPropertyKeys())
    	    		{
    	    			if (key.startsWith("#"))
    	    			{
    	    				String name = key.substring(1);
    	    				Project project;
    	    				if (projectsByName.containsKey(name))
    	    				{
    	    					project = projectsByName.get(name);
    	    				}
    	    				else
    	    				{
    	    					project = new Project(name);
    	    					projects.add(project);
    	    				}
    	    				Duration duration = Duration.parse((String)node.getProperty(key));
    	    				System.out.println("Project " + project.getName() + " has duration " + duration);
    	    				project.setCurrentDuration(duration);
        	    			if (project.getName().equals(activeName))
        	    			{
        	    				if (!isToday)
        	    				{
        	    					project.endOfDay();
        	    				}
        	    				else
        	    				{
	        	    				activeProject = project;
	        	    				project.setStartTime(startTime);
	        	    				project.setDuration(project.getCurrentDuration());
            	    				System.out.println("Project " + project.getName() + " has start time " + project.getStartTime());
        	    				}
        	    			}
    	    			}
    	    		}
    	    	}
    	    }
    	    tx.success();
        }
        return activeProject;
    }
    
    @Override
    public List<Project> readProjects()
    {
        List<Project> projects = new ArrayList<>();
    	try ( Transaction tx = graphDb.beginTx() )
    	{
    	    try ( ResourceIterator<Node> projectNodes =
    	    		GlobalGraphOperations.at(graphDb).getAllNodesWithLabel(projectLabel).iterator() )
    	    {
    	        while ( projectNodes.hasNext() )
    	        {
    	        	Node node = projectNodes.next();
    	        	String name = (String)node.getProperty("name");
    	        	System.out.println("Found project " + name);
    	        	Project project = new Project(name);
    	        	project.setId(node.getId());
    	            projects.add(project);
    	        }
    	    }
    	    
    	    tx.success();
    	}
    	return projects;
    }
    
    @Override
    public Map<String, Duration> report(LocalDate startDate, LocalDate endDate)
    {
    	Map<String, Duration> durations = new HashMap<>();
        try ( Transaction tx = graphDb.beginTx() )
        {
        	LocalDate date = startDate;
        	while (!date.isAfter(endDate))
        	{
//    			System.out.println("Loading timex data for " + date);

	    	    try ( ResourceIterator<Node> dateNodes =
	    	    		graphDb.findNodesByLabelAndProperty(dateLabel, "date", date.toString()).iterator() )
	    	    {
	    	    	while (dateNodes.hasNext())
	    	    	{
	    	    		Node node = dateNodes.next();
	    	    		for (String key : node.getPropertyKeys())
	    	    		{
	    	    			if (key.startsWith("#"))
	    	    			{
	    	    				String name = key.substring(1);
	    	    				Duration duration;
	    	    				if (!durations.containsKey(name))
	    	    				{
	    	    					duration = Duration.ZERO;
	    	    				}
	    	    				else
	    	    				{
	    	    					duration = durations.get(name);
	    	    				}
	    	    				Duration newDuration = duration.plus(Duration.parse((String)node.getProperty(key)));
	    	    				durations.put(name, newDuration);
	    	    			}
	    	    		}
	    	    	}
	    	    }
	    	    date = date.plusDays(1);
        	}
        	tx.success();
        }
        return durations;
    }
    
    private static void registerShutdownHook( final GraphDatabaseService graphDb )
    {
        // Registers a shutdown hook for the Neo4j instance so that it
        // shuts down nicely when the VM exits (even if you "Ctrl-C" the
        // running application).
        Runtime.getRuntime().addShutdownHook( new Thread()
        {
            @Override
            public void run()
            {
                graphDb.shutdown();
            }
        } );
    }

    @Override
	public void add(Project project) 
	{
        try ( Transaction tx = graphDb.beginTx() )
        {
        	Node projectNode = graphDb.createNode();
        	projectNode.setProperty( "name", project.getName());
        	projectNode.addLabel(projectLabel);
        	
            tx.success();
            project.setId(projectNode.getId());
        	System.out.println("Added project " + project.getName() 
        			+ " with id = " + project.getId());
        }
	}

    @Override
	public void remove(Project project) 
	{
    	try ( Transaction tx = graphDb.beginTx() )
    	{
    		Node node = graphDb.getNodeById(project.getId());
    		node.delete();
    		
    		tx.success();
    	}
		System.out.println("Removed project " + project.getName());
	}
}
