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
            File imageFile = new File("path/to/your/image.jpg");
            InetAddress serverAddress = InetAddress.getByName("REMOTE_IP_ADDRESS"); // Replace with your server IP
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
}

