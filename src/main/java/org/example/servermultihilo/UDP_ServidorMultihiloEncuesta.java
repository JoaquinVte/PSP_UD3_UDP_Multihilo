package org.example.servermultihilo;

import org.example.model.ResultadosEncuesta;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class HiloRegistroRespuesta implements Runnable {

    final ResultadosEncuesta resultados;
    final String zona;
    final String respuesta;

    HiloRegistroRespuesta(ResultadosEncuesta resultados, String zona, String respuesta) {
        this.resultados = resultados;
        this.zona = zona;
        this.respuesta = respuesta;
    }

    @Override
    public void run() {
        try {
            Thread.sleep(20);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        resultados.anotaRespuesta(zona, respuesta);
        try {
            Thread.sleep(20);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }
}



class HiloFinDatosZona implements Runnable {
    private final static String COD_TEXTO = "UTF-8";

    final DatagramSocket serverSocket;
    final DatagramPacket paqueteRecibido;
    final ResultadosEncuesta resultados;
    final String zona;

    HiloFinDatosZona(ResultadosEncuesta resultados, String zona,  DatagramSocket serverSocket, DatagramPacket paqueteRecibido) {

        this.resultados = resultados;
        this.zona = zona;
        this.serverSocket = serverSocket;
        this.paqueteRecibido = paqueteRecibido;

    }



    @Override

    public void run() {
        String resultEncuesta = "@result#" + zona + "#" + resultados.obtenNumRespuestasZona(zona) + "@";

        byte[] b;
        try {
            b = resultEncuesta.getBytes(COD_TEXTO);
            DatagramPacket paqueteEnviado = new DatagramPacket(b, b.length, paqueteRecibido.getAddress(), paqueteRecibido.getPort());
            serverSocket.send(paqueteEnviado);
        } catch (IOException ex) {
            System.out.printf("ERROR: en servidor con fin de datos para zona %s\n", zona);
            ex.printStackTrace();
        }
    }
}



class HiloResultados implements Runnable {

    private final static String COD_TEXTO = "UTF-8";
    final DatagramSocket serverSocket;
    final DatagramPacket paqueteRecibido;
    final ResultadosEncuesta resultados;

    HiloResultados(ResultadosEncuesta resultados, DatagramSocket serverSocket, DatagramPacket paqueteRecibido) {

        this.resultados = resultados;
        this.serverSocket = serverSocket;
        this.paqueteRecibido = paqueteRecibido;
    }

   @Override
    public void run() {
        try {
            String datosResultados = resultados.obtenResultadosXML();
            byte[] b = datosResultados.getBytes(COD_TEXTO);
            DatagramPacket paqueteEnviado = new DatagramPacket(b, b.length, paqueteRecibido.getAddress(), paqueteRecibido.getPort());
            serverSocket.send(paqueteEnviado);
        } catch (IOException ex) {
            System.out.printf("ERROR: en servidor con resultados\n");
            ex.printStackTrace();
        }
    }
}



public class UDP_ServidorMultihiloEncuesta {

    private final static int MAX_BYTES = 1500;
    private final static String COD_TEXTO = "UTF-8";

    public static void main(String[] args) {

        if (args.length < 1) {
            System.err.println("ERROR, indicar: puerto.");
            System.exit(1);
        }

        int numPuerto = Integer.parseInt(args[0]);
        ResultadosEncuesta resultados = new ResultadosEncuesta();

        try (DatagramSocket serverSocket = new DatagramSocket(numPuerto)) {

            System.out.printf("Creado socket de datagramas para puerto %s.\n", numPuerto);

            while (true) {

                byte[] datosRecibidos = new byte[MAX_BYTES];
                DatagramPacket paqueteRecibido = new DatagramPacket(datosRecibidos, datosRecibidos.length);

                serverSocket.receive(paqueteRecibido);

                String mensaje = new String(paqueteRecibido.getData(),0, paqueteRecibido.getLength(), COD_TEXTO);

                // Analizar contenido del mensaje
                String zona, respuesta;
                Pattern patRespuesta = Pattern.compile("@resp#(.+)#(.*)@");
                Matcher m = patRespuesta.matcher(mensaje);
                if (m.find()) {  // Respuesta para la zona
                    zona = m.group(1);
                    respuesta = m.group(2);
                    Thread hiloServicio = new Thread( new HiloRegistroRespuesta(resultados, zona, respuesta));
                    hiloServicio.start();
                } else {  // Fin de resultados para la zona
                    Pattern patFin = Pattern.compile("@fin#(.+)@");
                    m = patFin.matcher(mensaje);
                    if (m.find()) {  // Responder con resultados de la zona
                        zona = m.group(1);
                        Thread hiloServicio = new Thread(new HiloFinDatosZona(resultados, zona, serverSocket, paqueteRecibido));
                        hiloServicio.start();
                    } else if (mensaje.equals("@resultados@")) {
                        Thread hiloServicio = new Thread(new HiloResultados(resultados, serverSocket, paqueteRecibido));
                        hiloServicio.start();
                    }
                }
            }

        } catch (SocketException ex) {
            System.out.println("Excepción de sockets");
            ex.printStackTrace();
        } catch (IOException ex) {
            System.out.println("Excepción de E/S");
            ex.printStackTrace();
        }
    }
}
