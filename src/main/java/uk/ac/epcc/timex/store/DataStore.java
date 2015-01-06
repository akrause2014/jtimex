package uk.ac.epcc.timex.store;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import uk.ac.epcc.timex.Project;

public interface DataStore
{
	void init();
	
	List<Project> readProjects();
	
    void add(Project project);
    
    void remove(Project project);
    
    void storeTimexData(LocalDate date, List<Project> projects, Project activeProject);
    
    Project loadTimexData(LocalDate date, List<Project> projects, boolean active);
    
    Map<String, Duration> report(LocalDate startDate, LocalDate endDate);

}
