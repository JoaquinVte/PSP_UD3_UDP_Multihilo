package org.example;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Random;

public class UDP_Encuesta_cliente {

    private final static int MAX_BYTES = 1500;
    private final static String COD_TEXTO = "UTF-8";

    public static void main(String[] args) {

        if (args.length < 3) {
            System.err.println("ERROR, indicar: id_zona host_servidor puerto");
            System.exit(1);
        }

        String idZona = args[0];
        String nomHost = args[1];
        int numPuerto = Integer.parseInt(args[2]);

        try (DatagramSocket clientSocket = new DatagramSocket()) {

            InetAddress IPServidor = InetAddress.getByName(nomHost);
            clientSocket.connect(IPServidor, numPuerto);

            System.out.printf(">>Encuestador para zona %s comienza.\n", idZona);

            final Random r = new Random();
            int numRespuestas = 100 + r.nextInt(200 - 100) + 1;


            String mensaje;  // Mensaje que se va a enviar
            byte[] b;        // Array de byte[] para mensaje codificado en UTF-8

            long start = System.currentTimeMillis();

            for (int i = 0; i < numRespuestas; i++) {

                int numRespuesta = r.nextInt(10);   // Respuesta de 0 a 9, 0 es NS/NC
                String respuesta = "";  // NS/NC

                if (numRespuesta > 0) {
                    respuesta = "respuesta_" + numRespuesta;
                }

                // Envía resultado en datagrama UDP a servidor
                mensaje = "@resp#" + idZona + "#" + respuesta + "@";
                b = mensaje.getBytes(COD_TEXTO);

                DatagramPacket paqueteEnviado = new DatagramPacket(b, b.length, IPServidor, numPuerto);
                clientSocket.send(paqueteEnviado);
            }

            // Envía datagrama indicando que se ha terminado
            mensaje = "@fin#" + idZona + "@";
            b = mensaje.getBytes(COD_TEXTO);
            DatagramPacket paqueteEnviado = new DatagramPacket(b, b.length, IPServidor, numPuerto);
            clientSocket.send(paqueteEnviado);

            // Recibe datagrama de confirmación del servidor
            byte[] datosRecibidos = new byte[MAX_BYTES];

            DatagramPacket paqueteRecibido = new DatagramPacket(datosRecibidos, datosRecibidos.length);
            clientSocket.receive(paqueteRecibido);

            String respuesta = new String(paqueteRecibido.getData(),0, paqueteRecibido.getLength(), COD_TEXTO);
            System.out.printf("Respuesta del servidor: %s\n", respuesta);

            System.out.printf("##Encuestador para zona %s termina.\n", idZona);

            long end = System.currentTimeMillis();

            System.out.println("Tiempo empleado: " + (end-start) +" ms");

        } catch (SocketException ex) {
            System.out.println("Excepción de sockets");
            ex.printStackTrace();
        } catch (IOException ex) {
            System.out.println("Excepción de E/S");
            ex.printStackTrace();
        }
    }
}