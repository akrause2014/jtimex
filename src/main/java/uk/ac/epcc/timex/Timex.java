package uk.ac.epcc.timex;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import org.controlsfx.dialog.Dialogs;

import uk.ac.epcc.timex.store.DataStore;
import uk.ac.epcc.timex.store.InitialisationFailed;
import uk.ac.epcc.timex.store.Neo4JStore;

public class Timex extends Application {
	
    private volatile Project activeProject = null;
    
    public static void main(String[] args) {
        launch(args);
    }
 
    @Override
    public void start(Stage stage) 
    {
        ObservableList<Project> data = FXCollections.observableArrayList();
        BorderPane tablePane = new BorderPane();

    	DataStore store = new Neo4JStore();
    	try 
    	{
    		store.init();
    	}
    	catch (InitialisationFailed e)
    	{
    		printMessages(e);
    		Platform.exit();
    		return;
    	}
    	data.addAll(store.readProjects());
    	activeProject = store.loadTimexData(LocalDate.now(), data, true);
    	
        Scene scene = new Scene(new Group());
        stage.setTitle("Timex");
        
        stage.setWidth(300);
        stage.setHeight(400);

        MenuBar menuBar = new MenuBar();
        Menu trackerMenu = new Menu("Tracker");
        MenuItem pauseItem = new MenuItem("Pause");
        pauseItem.setOnAction(new EventHandler<ActionEvent>() {
        	@Override
        	public void handle(ActionEvent e)
        	{
        		TableView<Project> table = getTable(tablePane);
        		table.getSelectionModel().clearSelection();
        	}
        });
        MenuItem quitItem = new MenuItem("Quit");
        quitItem.setOnAction(new EventHandler<ActionEvent>() {
        	@Override
        	public void handle(ActionEvent e) {
        		TableView<Project> table = getTable(tablePane);
        		table.getSelectionModel().clearSelection();
        		stage.close();
        	}
        });
        trackerMenu.getItems().addAll(pauseItem, quitItem);
        Menu projectMenu = new Menu("Projects");
        MenuItem addProjectItem = new MenuItem("Add Project...");
        addProjectItem.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
            	ObservableList<Project> projects = getTableItems(tablePane);
            	Optional<String> result = Dialogs.create()
            			.owner(stage)
            			.title("New Project")
            			.message("Please enter a project name:")
            			.showTextInput();
            	if (!result.isPresent()) return;
            	String name = result.get();
            	if (name.isEmpty())
            	{
            		Dialogs.create()
                    .owner(stage)
                    .title("Empty project name")
                    .message("Please enter a valid project name.")
                    .showError();
            		return;
            	}
            	for (Project p : projects)
            	{
            		if (p.getName().equals(name))
            		{
            			Dialogs.create()
            			.owner(stage)
            			.title("Duplicate project")
            			.message("A project named '" + name + "' already exists.\nPlease enter a new project name.")
            			.showError();
            			return;
            		}
            	}
            	Project project = new Project(name);
            	projects.add(project);
            	store.add(project);
            }
        });

        MenuItem removeProjectItem = new MenuItem("Delete Selected");
        removeProjectItem.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
            	TableView<Project> table = getTable(tablePane);
            	Project selected = table.getSelectionModel().getSelectedItem();
            	if (selected == null) return;
            	table.getSelectionModel().clearSelection();
            	ObservableList<Project> projects = table.getItems();
            	projects.remove(selected);
            	store.remove(selected);
            	store.storeTimexData(LocalDate.now(), projects, null);
            }
        });

        Menu reportMenu = ReportBuilder.createReportMenu(store);

        projectMenu.getItems().addAll(addProjectItem, removeProjectItem);
        menuBar.getMenus().addAll(trackerMenu, projectMenu, reportMenu);
        
        TableView<Project> table = createTableView();
        table.setItems(data);
                
        final Button editButton = new Button("Edit Table");
        DatePicker timexDatePicker = new DatePicker();
        timexDatePicker.setValue(LocalDate.now());
        // previous value
        timexDatePicker.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e)
            {
            	ObservableList<Project> projects = FXCollections.observableArrayList();
            	LocalDate date = timexDatePicker.getValue();
            	boolean newDateIsToday = date.isEqual(LocalDate.now());
            	if (newDateIsToday)
            	{
            		projects.addAll(store.readProjects());
            	}
            	activeProject = store.loadTimexData(date, projects, newDateIsToday);
            	TableView<Project> newTable = createTableView();
            	newTable.setItems(projects);
            	if (newDateIsToday)
            	{
            		if (activeProject != null)	
            		{
                    	newTable.getSelectionModel().clearSelection();
                		newTable.getSelectionModel().select(activeProject);
            		}
            		addSelectionListener(newTable, store);
            		tablePane.setBottom(editButton);
            		trackerMenu.setDisable(false);
            	}
            	else
            	{
            		newTable.getSelectionModel().clearSelection();
            		makeEditable(newTable, store, timexDatePicker);
            		if (tablePane.getBottom() != null)
            		{
            			if (editButton.getText().startsWith("Exit"))
            			{
            				editButton.fire();
            			}
            			tablePane.setBottom(null);
            		}
            		trackerMenu.setDisable(true);
            	}
            	tablePane.setCenter(newTable);
            }
        });
        Button previousDayBt = new Button("<");
        previousDayBt.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
            	timexDatePicker.setValue(timexDatePicker.getValue().minusDays(1));
            }
        });
        Button nextDayBt = new Button(">");
        nextDayBt.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
            	timexDatePicker.setValue(timexDatePicker.getValue().plusDays(1));
            }
        });
        BorderPane dateControlB = new BorderPane();
        dateControlB.setLeft(previousDayBt);
        dateControlB.setRight(nextDayBt);
        dateControlB.setCenter(timexDatePicker);
        dateControlB.setPadding(new Insets(0, 0, 10, 0));
        
        if (activeProject != null)
        {
        	table.getSelectionModel().clearSelection();
        	table.getSelectionModel().select(activeProject);
        }
        addSelectionListener(table, store);
        
        String defaultStyle = table.getStyle();
        editButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
            	TableView<Project> oldTable = getTable(tablePane);
        		Project selectedProject = oldTable.getSelectionModel().getSelectedItem();
            	oldTable.getSelectionModel().clearSelection();
            	TableView<Project> newTable = createTableView();
            	newTable.setItems(oldTable.getItems());
            	tablePane.setCenter(newTable);
            	if ((Boolean) editButton.getText().equals("Exit Edit Mode"))
            	{
            		System.out.println("Exiting edit mode");
                	newTable.setStyle(defaultStyle);
                	LocalDate date = timexDatePicker.getValue();
                	if (LocalDate.now().isEqual(date))
                	{
                		addSelectionListener(newTable, store);
                	}
                	else
                	{
                		store.storeTimexData(date, oldTable.getItems(), null);
                	}
	            	editButton.setText("Edit Table");
	            	Project actProj = (Project) editButton.getProperties().get("activeProject");
	            	if (actProj != null)
	            	{
	            		System.out.println("Selecting active project: " + actProj.getName());
	            		newTable.getSelectionModel().select(actProj);
	            	}
            	}
            	else
            	{
            		editButton.getProperties().put("activeProject", selectedProject); 
                	newTable.setStyle("-fx-control-inner-background: lightyellow;");
	            	makeEditable(newTable, store, timexDatePicker);
	            	tablePane.setCenter(newTable);
	            	editButton.setText("Exit Edit Mode");
            	}
            }
        });
 
        final HBox projectControlB = new HBox();
        projectControlB.getChildren().addAll(editButton);
        projectControlB.setSpacing(3);
 
        tablePane.setTop(dateControlB);
        tablePane.setCenter(table);
        tablePane.setBottom(projectControlB);
        final VBox vbox = new VBox();
        vbox.setSpacing(10);
        vbox.setPadding(new Insets(10));
        vbox.getChildren().addAll(menuBar, tablePane);
        
        VBox.setVgrow(table, Priority.ALWAYS);
 
//        vbox.setPrefSize(300, 300);
        
        scene.setRoot(vbox);
//        ((Group) scene.getRoot()).getChildren().addAll(vbox);
 
        stage.setScene(scene);
        stage.show();
        Timer timer = new Timer(true);
        TimerTask timerTask = new TimerTask() {
			
			@Override
			public void run() {
				if (activeProject != null)
				{
					activeProject.tick();
				}
			}
		};
        timer.scheduleAtFixedRate(timerTask, 1000, 1000);
    }

	private void printMessages(Throwable e)
	{
		System.err.println(e.getMessage());
		Throwable exc = e.getCause();
		while (exc != null)
		{
			System.err.println(exc.getMessage());
			exc = exc.getCause();
		}
	}
	
	TableView<Project> createTableView()
	{
		TableView<Project> table = new TableView<Project>();
        table.setEditable(true);
    
        TableColumn<Project, String> nameCol = new TableColumn<>("Project");
        nameCol.setCellValueFactory(
                new PropertyValueFactory<Project, String>("name"));
        nameCol.prefWidthProperty().bind(table.widthProperty().multiply(.7));
 
        TableColumn<Project, String> durationCol = new TableColumn<>("Duration");
        durationCol.setCellValueFactory(
                new PropertyValueFactory<Project, String>("duration"));

//        durationCol.prefWidthProperty().bind(table.widthProperty().multiply(.3));
 
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getColumns().addAll(nameCol, durationCol);
        return table;
	}
	
	void makeEditable(TableView<Project> table, DataStore store, DatePicker datePicker)
	{
		TableColumn<Project, String> nameCol = (TableColumn<Project, String>) table.getColumns().get(0);
        nameCol.setCellFactory(TextFieldTableCell.forTableColumn());
        nameCol.setOnEditCommit(
        	    new EventHandler<CellEditEvent<Project, String>>() {
        	        @Override
        	        public void handle(CellEditEvent<Project, String> t) {
        	            ((Project) t.getTableView().getItems().get(
        	                t.getTablePosition().getRow())
        	                ).setName(t.getNewValue());
        	        }
        	    });     
		TableColumn<Project, String> durationCol = (TableColumn<Project, String>) table.getColumns().get(1);
        durationCol.setCellFactory(TextFieldTableCell.forTableColumn());
        durationCol.setOnEditCommit(
        	    new EventHandler<CellEditEvent<Project, String>>() {
        	        @Override
        	        public void handle(CellEditEvent<Project, String> t) {
        	        	Project project =
        	        			(Project) t.getTableView().getItems().get(
        	        					t.getTablePosition().getRow());
        	        	try
        	        	{
        	        		project.editDuration(t.getNewValue());
        	        		store.storeTimexData(datePicker.getValue(), t.getTableView().getItems(), null);
        	        	}
        	        	catch (IllegalArgumentException e)
        	        	{
                			Dialogs.create()
                			.owner(table.getParent().getParent())
                			.title("Invalid input")
                			.message("Please enter the format <hh>:<mm>:<ss>.")
                			.showError();
                			t.getTableColumn().setVisible(false);
                			t.getTableColumn().setVisible(true);
        	        	}
        	        }
        	    });       
	}
	
	void addSelectionListener(TableView<Project> table, DataStore store)
	{
		ObservableList<Project> data = table.getItems();
        table.getSelectionModel().getSelectedIndices().addListener(
        	new ListChangeListener<Integer>()
        	{
        		@Override
        		public void onChanged(Change<? extends Integer> change)
        		{
        			while(change.next())
        			{
        				for (int i : change.getRemoved())
        				{
        					System.out.println("Deactivating " + data.get(i).getName());
        					data.get(i).deactivate();
        					activeProject = null;
        				}
        				for (int i : change.getAddedSubList())
        				{
        					System.out.println("Activating " + data.get(i).getName());
        					activeProject = data.get(i).activate();
        				}
        				store.storeTimexData(LocalDate.now(), data, activeProject);
        			}
        		}
        	});

	}

	TableView<Project> getTable(BorderPane tablePane)
	{
		return (TableView<Project>)tablePane.getCenter();
	}
	
	ObservableList<Project> getTableItems(BorderPane tablePane)
	{
		return getTable(tablePane).getItems();
	}
	
}
