package com.example.demo;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class DrawingServer extends Application {

    private static final int WINDOW_WIDTH = 500;
    private static final int WINDOW_HEIGHT = 500;
    private static final int SERVER_PORT = 1234;

    private GraphicsContext graphicsContext;
    private Map<String, Color> clientColors;
    private Map<String, Double[]> clientSegments;
    private double translationX;
    private double translationY;
    private Label translationLabel;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {

        clientColors = new HashMap<>();
        clientSegments = new HashMap<>();
        translationX = 0;
        translationY = 0;

        Canvas canvas = new Canvas(WINDOW_WIDTH, WINDOW_HEIGHT);
        graphicsContext = canvas.getGraphicsContext2D();
        graphicsContext.setFill(Color.WHITE);
        graphicsContext.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);

        translationLabel = new Label("Translation: (" + translationX + ", " + translationY + ")");
        HBox bottomBox = new HBox(10);
        bottomBox.setAlignment(Pos.CENTER);
        bottomBox.setPadding(new Insets(10));
        bottomBox.getChildren().add(translationLabel);

        BorderPane root = new BorderPane();
        root.setCenter(canvas);
        root.setBottom(bottomBox);

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Drawing Server");
        primaryStage.setResizable(false);
        primaryStage.show();

        startServer();

        scene.setOnKeyPressed(event -> {
            KeyCode keyCode = event.getCode();
            double translationDelta = 10;

            if (keyCode == KeyCode.UP) {
                translationY -= translationDelta;
            } else if (keyCode == KeyCode.DOWN) {
                translationY += translationDelta;
            } else if (keyCode == KeyCode.LEFT) {
                translationX -= translationDelta;
            } else if (keyCode == KeyCode.RIGHT) {
                translationX += translationDelta;
            }

            translationLabel.setText("Translation: (" + translationX + ", " + translationY + ")");
            redrawCanvas();
        });

        primaryStage.setOnCloseRequest(event -> System.exit(0));
    }

    private void startServer() {
        new Thread(() -> {
            try {
                ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
                System.out.println("Server started on port " + SERVER_PORT);

                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    handleClient(clientSocket);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void handleClient(Socket clientSocket) {
        new Thread(() -> {
            try {
                DataInputStream inputStream = new DataInputStream(clientSocket.getInputStream());

                String clientAddress = clientSocket.getInetAddress().getHostAddress();
                System.out.println("Client connected: " + clientAddress);

                while (true) {
                    if (inputStream.available() > 0) {
                        byte messageType = inputStream.readByte();

                        if (messageType == 0x01) { // Color message
                            handleColorMessage(inputStream, clientAddress);
                        } else if (messageType == 0x02) { // Segment message
                            handleSegmentMessage(inputStream, clientAddress);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void handleColorMessage(DataInputStream inputStream, String clientAddress) throws IOException {
        int colorValue = inputStream.readInt();
        Color color = Color.rgb((colorValue >> 16) & 0xFF, (colorValue >> 8) & 0xFF, colorValue & 0xFF);
        clientColors.put(clientAddress, color);
    }

    private void handleSegmentMessage(DataInputStream inputStream, String clientAddress) throws IOException {
        double x1 = inputStream.readDouble();
        double y1 = inputStream.readDouble();
        double x2 = inputStream.readDouble();
        double y2 = inputStream.readDouble();

        x1 += translationX;
        y1 += translationY;
        x2 += translationX;
        y2 += translationY;

        Color color = clientColors.getOrDefault(clientAddress, Color.BLACK);
        graphicsContext.setStroke(color);
        graphicsContext.strokeLine(x1, y1, x2, y2);

        // Dodaj segment do mapy clientSegments
        Double[] segment = {x1, y1, x2, y2};
        clientSegments.put(clientAddress, segment);
    }

    private void redrawCanvas() {
        graphicsContext.setFill(Color.WHITE);
        graphicsContext.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
        graphicsContext.setStroke(Color.BLACK);
        graphicsContext.setLineWidth(1.0);

        for (Map.Entry<String, Color> entry : clientColors.entrySet()) {
            String clientAddress = entry.getKey();
            Color color = entry.getValue();
            graphicsContext.setStroke(color);
            graphicsContext.setFill(color);
            graphicsContext.strokeText(clientAddress, 10, 20);

            // Rysuj segmenty dla konkretnego klienta
            if (clientSegments.containsKey(clientAddress)) {
                Double[] segment = clientSegments.get(clientAddress);
                double x1 = segment[0] + translationX;
                double y1 = segment[1] + translationY;
                double x2 = segment[2] + translationX;
                double y2 = segment[3] + translationY;
                graphicsContext.strokeLine(x1, y1, x2, y2);
            }
        }
    }
}