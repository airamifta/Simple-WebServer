/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package simplewebserver; // Deklarasi paket

import java.io.IOException; // Import kelas IOException
import java.net.ServerSocket; // Import kelas ServerSocket
import java.net.Socket; // Import kelas Socket
import java.util.List; // Import kelas List
import java.util.concurrent.ExecutorService; // Import kelas ExecutorService
import java.util.concurrent.Executors; // Import kelas Executors
import java.util.concurrent.atomic.AtomicBoolean; // Import kelas AtomicBoolean

/**
 *
 * @author Ramifta
 */

public class WebServer {
    private static String webRoot = "./webroot"; // Deklarasi dan inisialisasi variabel static webRoot
    private int port; // Deklarasi variabel instance untuk port
    private String logsPath; // Deklarasi variabel instance untuk logsPath
    private ExecutorService threadPool; // Deklarasi variabel instance untuk thread pool
    private AtomicBoolean running; // Deklarasi variabel instance untuk status running
    private ServerSocket serverSocket; // Deklarasi variabel instance untuk serverSocket

    public WebServer(String webRoot, String logsPath, int port) {
        WebServer.webRoot = webRoot; // Mengatur nilai static webRoot dengan parameter
        this.logsPath = logsPath; // Mengatur nilai logsPath dengan parameter
        this.port = port; // Mengatur nilai port dengan parameter
        this.threadPool = Executors.newCachedThreadPool(); // Inisialisasi thread pool
        this.running = new AtomicBoolean(false); // Inisialisasi status running dengan nilai false
    }

    public void start() {
        running.set(true); // Mengatur status running menjadi true
        try {
            serverSocket = new ServerSocket(port); // Membuat serverSocket pada port yang ditentukan
            System.out.println("Web server started on port " + port); // Mencetak pesan bahwa server telah dimulai
            while (running.get()) { // Loop utama server
                Socket clientSocket = serverSocket.accept(); // Menerima koneksi dari client
                threadPool.execute(new HttpRequestHandler(clientSocket, logsPath, this)); // Menangani request dari client menggunakan thread pool
            }
        } catch (IOException e) {
            if (running.get()) { // Jika server masih berjalan
                e.printStackTrace(); // Cetak stack trace jika terjadi kesalahan
            }
        } finally {
            stopServer(); // Memastikan server dihentikan dengan memanggil stopServer()
        }
    }

    public void stopServer() {
        running.set(false); // Mengatur status running menjadi false
        if (serverSocket != null && !serverSocket.isClosed()) { // Jika serverSocket tidak null dan belum ditutup
            try {
                serverSocket.close(); // Menutup serverSocket
            } catch (IOException e) {
                e.printStackTrace(); // Cetak stack trace jika terjadi kesalahan
            }
        }
        threadPool.shutdown(); // Menghentikan thread pool
        System.out.println("Web server stopped"); // Mencetak pesan bahwa server telah dihentikan
    }

    public boolean isAlive() {
        return running.get(); // Mengembalikan status running
    }

    public void setLogsPath(String logsPath) {
        this.logsPath = logsPath; // Mengatur nilai logsPath
    }

    public List<String> loadAccessLogs() {
        return new HttpRequestHandler(null, logsPath, this).loadAccessLogs(); // Memuat log akses menggunakan HttpRequestHandler
    }

    public static String getWebRoot() {
        return webRoot; // Mengembalikan nilai webRoot
    }
}
