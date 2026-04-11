import java.util.HashMap;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class ServerGUI extends Application {
    HashMap<String,Scene> scenes;
    Server serverConeection;
    ListView serverLog;

    public static void main(String[] args) { launch(args); }

    @Override
    public void start(Stage primaryStage) throws Exception {
        serverLog = new ListView<String>();
        serverConeection = new Server(data -> {
            Platform.runLater(() -> {
                String msg = data;
                serverLog.getItems().add(msg);
            });
        });

        scenes = new HashMap<String, Scene>();

        scenes.put("Server Log", createServerGUI());

        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent t) {
                Platform.exit();
                System.exit(0);
            }
        });

        primaryStage.setScene(scenes.get("Server Log"));
        primaryStage.setTitle("Server Log");
        primaryStage.show();
    }

    public Scene createServerGUI() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(70));

        pane.setCenter(serverLog);
        return new Scene(root, 500,400);
    }
}