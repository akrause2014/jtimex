package uk.ac.epcc.timex.store;

import java.io.File;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jtimex.Project;
import jtimex.store.Neo4JStore;
import junit.framework.TestCase;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class Neo4JStoreTest {

	final static String DB_PATH = "target/testdb";
	private static Neo4JStore store;

	@BeforeClass
	public static void setup()
	{
		store = new Neo4JStore(DB_PATH);
		store.init();
	}
	
	@AfterClass
	public static void tearDown()
	{
		new File(DB_PATH).delete();
	}
	
	@Test
	public void testStoreMonth() 
	{
		List<Project> projects = Arrays.asList(new Project("A"), new Project("B"), new Project("C"));
		Random random = new Random();
		for (Project p : projects)
		{
			store.add(p);
			p.setCurrentDuration(Duration.ofMinutes(random.nextInt(420)));
		}
		LocalDate startDate = LocalDate.parse("2014-01-01");
		LocalDate endDate = LocalDate.parse("2014-01-31");
		LocalDate date = startDate;
		while (date.isBefore(endDate))
		{
			store.storeTimexData(date, projects, null);
			date = date.plusDays(1);
		}
		
		List<Project> actual = new ArrayList<Project>(projects);
		date = startDate;
		while (date.isBefore(endDate))
		{
			store.loadTimexData(date, projects, false);
			for (int i=0; i<projects.size(); i++)
			{
				TestCase.assertEquals(projects.get(i).getCurrentDuration(), actual.get(i).getCurrentDuration());
			}
			date = date.plusDays(1);
		}
	}

	@Test
	public void testReportMonth() 
	{
		String dbPath = "target/testdb";
		try 
		{
			List<Project> projects = Arrays.asList(new Project("A"), new Project("B"), new Project("C"));
			Random random = new Random();
			for (Project p : projects)
			{
				store.add(p);
				p.setCurrentDuration(Duration.ofMinutes(random.nextInt(420)));
			}
			LocalDate startDate = LocalDate.parse("2014-01-01");
			LocalDate endDate = LocalDate.parse("2014-01-31");
			LocalDate date = startDate;
			while (!date.isAfter(endDate)) // end is inclusive
			{
				store.storeTimexData(date, projects, null);
				date = date.plusDays(1);
			}
			Map<String, Duration> durations = store.report(startDate, endDate);
			System.out.println(durations);
		}
		finally
		{
			new File(dbPath).delete();
		}
	}
	
}
