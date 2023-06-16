package com.example.demo;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class DrawingClient {
    private static final String SERVER_ADDRESS = "localhost"; // Adres serwera
    private static final int SERVER_PORT = 1234; // Port serwera

    public static void main(String[] args) {
        try {
            Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());

            Scanner scanner = new Scanner(System.in);

            while (true) {
                System.out.println("Wpisz parametry: x1 y1 x2 y2 color\nFormat: <liczba> <liczba> <liczba> <liczba> <6 cyfr>");
                System.out.print("Input: ");
                String command = scanner.nextLine();

                if (command.equalsIgnoreCase("exit")) {
                    break;
                }

                try {
                    String[] coordinates = command.split(" ");
                    if (coordinates.length != 5) {
                        System.out.println("Błędny format komendy!");
                        continue;
                    }
                    double x1 = Double.parseDouble(coordinates[0]);
                    double y1 = Double.parseDouble(coordinates[1]);
                    double x2 = Double.parseDouble(coordinates[2]);
                    double y2 = Double.parseDouble(coordinates[3]);
                    String color = coordinates[4];

                    outputStream.writeByte(0x01); // Color message
                    outputStream.writeInt(Integer.parseInt(color, 16));
                    outputStream.writeByte(0x02); // Segment message
                    outputStream.writeDouble(x1);
                    outputStream.writeDouble(y1);
                    outputStream.writeDouble(x2);
                    outputStream.writeDouble(y2);
                    outputStream.flush();
                } catch (NumberFormatException e) {
                    System.out.println("Błędny format danych!");
                } catch (IOException e) {
                    System.out.println("Błąd podczas wysyłania komendy!");
                }
            }

            socket.close();
        } catch (IOException e) {
            System.out.println("Błąd podczas połączenia z serwerem!");
        }
    }
}