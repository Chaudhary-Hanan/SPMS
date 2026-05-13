package com.spms.app;

import com.spms.service.DatabaseService;
import com.spms.view.MainWindow;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Application entry point for the Student Productivity Management System.
 */
public class SPMSApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Initialise SQLite database (creates tables + sample data on first run)
        DatabaseService.getInstance().initialize();

        // Build and show the main window
        new MainWindow(primaryStage).show();
    }

    @Override
    public void stop() {
        DatabaseService.getInstance().close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
