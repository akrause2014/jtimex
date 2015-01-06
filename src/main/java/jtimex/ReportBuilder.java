package jtimex;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import jtimex.store.DataStore;

import org.controlsfx.dialog.Dialogs;

import com.eclipsesource.json.JsonObject;

public class ReportBuilder 
{
	public static void openReportDialog(final DataStore store)
	{
    	Stage stage = new Stage();
    	final Label label = new Label("Select dates:");
    	label.setPadding(new Insets(5));
        label.setFont(new Font("Arial", 16));

    	final GridPane gridDates = new GridPane();
    	gridDates.setStyle("-fx-border-style: solid; -fx-border-width: 1; -fx-border-color: gray");
    	gridDates.setHgap(10); 
    	gridDates.setVgap(10); 
    	gridDates.setPadding(new Insets(10, 10, 10, 10)); 
    	final Label labelFrom = new Label("Start Date");
    	gridDates.add(labelFrom, 0, 0);
    	final DatePicker datePickerFrom = new DatePicker();
    	gridDates.add(datePickerFrom, 1, 0);
    	final Label labelTo = new Label("End Date");
    	gridDates.add(labelTo, 0, 1);
    	final DatePicker datePickerTo = new DatePicker();
        final Callback<DatePicker, DateCell> dayCellFactory = 
                new Callback<DatePicker, DateCell>() {
                    @Override
                    public DateCell call(final DatePicker datePicker) {
                        return new DateCell() {
                            @Override
                            public void updateItem(LocalDate item, boolean empty) {
                                super.updateItem(item, empty);
                               
                                LocalDate startDate = datePickerFrom.getValue();
                                if (startDate != null && item.isBefore(startDate)) {
                                	setDisable(true);
                                	setStyle("-fx-background-color: #ffc0cb;");
                                }   
                            }
                        };
                    }
            	};
        datePickerTo.setDayCellFactory(dayCellFactory);
    	gridDates.add(datePickerTo, 1, 1);

    	Button thisWeek = new Button("This Week");
    	thisWeek.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				LocalDate today = LocalDate.now();
				LocalDate monday = today.with(
						TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
				LocalDate sunday = today.with(
						TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
				datePickerFrom.setValue(monday);
				datePickerTo.setValue(sunday);
			}
		});
    	Button lastWeek = new Button("Last Week");
    	lastWeek.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				LocalDate today = LocalDate.now().minusWeeks(1);
				LocalDate monday = today.with(
						TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
				LocalDate sunday = today.with(
						TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
				datePickerFrom.setValue(monday);
				datePickerTo.setValue(sunday);
			}
		});
    	Button thisMonth = new Button("This Month");
    	thisMonth.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				LocalDate today = LocalDate.now();
				LocalDate first = today.with(
						TemporalAdjusters.firstDayOfMonth());
				LocalDate last = today.with(
						TemporalAdjusters.lastDayOfMonth());
				datePickerFrom.setValue(first);
				datePickerTo.setValue(last);
			}
		});

    	Button lastMonth = new Button("Last Month");
    	lastMonth.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				LocalDate monthAgo = LocalDate.now().minusMonths(1);
				LocalDate first = monthAgo.with(
						TemporalAdjusters.firstDayOfMonth());
				LocalDate last = monthAgo.with(
						TemporalAdjusters.lastDayOfMonth());
				datePickerFrom.setValue(first);
				datePickerTo.setValue(last);
			}
		});
    	
    	Label formatLabel = new Label("Formatting: ");
    	ObservableList<String> formatOptions = 
    		    FXCollections.observableArrayList(
    		        "Table",
    		        "CSV",
    		        "JSON"
    		    );
    	final ComboBox<String> formatCB = new ComboBox<>(formatOptions);
    	formatCB.getSelectionModel().select(0);
    	GridPane grid = new GridPane();
    	grid.add(formatLabel, 0, 0);
    	grid.add(formatCB, 1, 0);

    	Button ok = new Button("Create Report");
    	ok.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
            	LocalDate startDate = datePickerFrom.getValue();
            	LocalDate endDate = datePickerTo.getValue();
            	String format = formatCB.getValue();
            	if (startDate == null || endDate == null)
            	{
            		Dialogs.create()
                    .owner(stage)
                    .title("Missing start or end date")
                    .message("Please enter a valid date range.")
                    .showError();
            	}
            	else
            	{
	            	Map<String, Duration> report = store.report(startDate, endDate);
	            	displayReport(format, startDate, endDate, report);
	            	stage.close();
            	}
            }
    	});
    	Button cancel = new Button("Cancel");
    	cancel.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
            	stage.close();
            }
    	});
    	final HBox hbCommand = new HBox(ok, cancel);
    	hbCommand.setPadding(new Insets(10, 0, 10, 0)); 
    	hbCommand.setAlignment(Pos.CENTER);
    	HBox hbPrepRange = new HBox(thisWeek, lastWeek, thisMonth, lastMonth);
    	hbPrepRange.setPadding(new Insets(10, 0, 0, 0)); 
    	VBox vbox = new VBox(label, gridDates, hbPrepRange, grid, hbCommand);
    	vbox.setPadding(new Insets(10)); 

    	Scene s = new Scene(vbox);
    	stage.setScene(s);
    	stage.initModality(Modality.WINDOW_MODAL);
    	stage.show();
	}
	
	public static void displayReport(String format, LocalDate startDate, LocalDate endDate, Map<String, Duration> report)
	{
		if (format.equals("Table"))
		{
			displayReportAsTable(startDate, endDate, report);
		}
		else if (format.equals("JSON"))
		{
			JsonObject json = generateJSON(startDate, endDate, report);
			displayReportAsText(json.toString());

		}
		else if (format.equals("CSV"))
		{
			String csv = generateCSV(startDate, endDate, report);
			displayReportAsText(csv);
		}
	}
	
	private static String generateCSV(LocalDate startDate,
			LocalDate endDate, Map<String, Duration> report)
	{
		StringBuilder builder = new StringBuilder();
		report.forEach((n,d) -> {
			builder.append(n).append(",");
			builder.append(Project.getDurationAsString(d));
			builder.append("\n");
		});
		return builder.toString(); 
	}

	private static JsonObject generateJSON(LocalDate startDate,
			LocalDate endDate, Map<String, Duration> report) 
	{
		JsonObject obj = new JsonObject();
		report.forEach((n,d) -> {
			obj.add(n, Project.getDurationAsString(d));
		});
		return obj;
	}

	private static void displayReportAsText(String value) 
	{
		TextArea text = new TextArea();
		text.setEditable(false);
		text.setText(value);
    	Stage stage = new Stage();
    	Scene scene = new Scene(text);
        stage.setScene(scene);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.show();

	}

	public static void displayReportAsTable(
			LocalDate startDate, 
			LocalDate endDate, 
			Map<String, Duration> report)
	{
		final Label dates = new Label("Start: " + startDate + ", end: " + endDate);
		dates.setPadding(new Insets(10));
		
	    final ObservableList<Project> data =
	            FXCollections.observableArrayList(
	            );
		report.forEach((k,v) -> { 
			Project project = new Project(k); 
			project.setDuration(v); 
			data.add(project); 
		});
		final TableView<Project> table = new TableView<Project>();
        TableColumn<Project, String> nameCol = new TableColumn<>("Project");
        nameCol.setCellValueFactory(
                new PropertyValueFactory<Project, String>("name"));
        TableColumn<Project, String> durationCol = new TableColumn<>("Duration");
        durationCol.setCellValueFactory(
                new PropertyValueFactory<Project, String>("duration"));
       
        table.setItems(data);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getColumns().addAll(nameCol, durationCol);
    	Stage stage = new Stage();
    	MenuBar menuBar = new MenuBar();
    	Menu menuFile = new Menu("File");
        MenuItem menuJson = new MenuItem("Save as JSON");
        menuJson.setOnAction(new EventHandler<ActionEvent>() {
        	public void handle(ActionEvent t) {
        		JsonObject obj = generateJSON(startDate, endDate, report);
        		System.out.println(obj.toString());
        		writeToFile(obj.toString(), stage);
        	}
        });     
        MenuItem menuCSV = new MenuItem("Save as CSV");
        menuCSV.setOnAction(new EventHandler<ActionEvent>() {
        	public void handle(ActionEvent t) {
        		String csv = generateCSV(startDate, endDate, report);
        		writeToFile(csv, stage);
        	}
        });        
        menuFile.getItems().addAll(menuJson, menuCSV);
     
        menuBar.getMenus().addAll(menuFile);
 
    	VBox vbox = new VBox(menuBar, dates, table);
    	Scene scene = new Scene(vbox);
        stage.setScene(scene);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.show();
	}
	
	public static Menu createReportMenu(DataStore store)
	{
        Menu reportMenu = new Menu("Report");
        MenuItem lastWeekItem = new MenuItem("Last Week");
        lastWeekItem.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
				LocalDate today = LocalDate.now().minusWeeks(1);
				LocalDate startDate = today.with(
						TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
				LocalDate endDate = today.with(
						TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
            	ReportBuilder.displayReport(
            			"Table", 
            			startDate, endDate, 
            			store.report(startDate, endDate));
            }
        });
        MenuItem thisWeekItem = new MenuItem("This Week");
        thisWeekItem.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
				LocalDate today = LocalDate.now();
				LocalDate startDate = today.with(
						TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
				LocalDate endDate = today.with(
						TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
            	ReportBuilder.displayReport(
            			"Table", 
            			startDate, endDate, 
            			store.report(startDate, endDate));
            }
        });
        MenuItem thisMonthItem = new MenuItem("This Month");
        thisMonthItem.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
				LocalDate today = LocalDate.now();
				LocalDate startDate = today.with(
						TemporalAdjusters.firstDayOfMonth());
				LocalDate endDate = today.with(
						TemporalAdjusters.lastDayOfMonth());
            	ReportBuilder.displayReport(
            			"Table", 
            			startDate, endDate, 
            			store.report(startDate, endDate));
            }
        });
        MenuItem lastMonthItem = new MenuItem("Last Month");
        lastMonthItem.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
				LocalDate today = LocalDate.now().minusMonths(1);
				LocalDate startDate = today.with(
						TemporalAdjusters.firstDayOfMonth());
				LocalDate endDate = today.with(
						TemporalAdjusters.lastDayOfMonth());
            	ReportBuilder.displayReport(
            			"Table", 
            			startDate, endDate, 
            			store.report(startDate, endDate));
            }
        });
        MenuItem createItem = new MenuItem("Custom");
        reportMenu.getItems().addAll(
        		thisWeekItem, 
        		thisMonthItem, 
        		lastWeekItem, 
        		lastMonthItem, 
        		createItem);
        createItem.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
            	ReportBuilder.openReportDialog(store);
            }
        });
        return reportMenu;
	}
	
	
	protected static void writeToFile(String content, Stage stage) 
	{
		FileChooser fileChooser = new FileChooser();
		File file = fileChooser.showSaveDialog(stage);
		if (file == null)
		{
			// user cancelled
			return;
		}
		try ( FileWriter writer = new FileWriter(file) )
		{
    		writer.write(content);
		} 
		catch (IOException e) 
		{
			System.out.println("Could not write file " + file);
		}
	}
	
	
	
}
