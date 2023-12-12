package org.example.server;

import org.example.model.ResultadosEncuesta;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UDP_Encuesta_servidor {

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
            Random r = new Random();
            while (true) {

                byte[] datosRecibidos = new byte[MAX_BYTES];
                DatagramPacket paqueteRecibido = new DatagramPacket(datosRecibidos, datosRecibidos.length);

                byte[] b;
                DatagramPacket paqueteEnviado;

                serverSocket.receive(paqueteRecibido);

                String mensaje = new String(paqueteRecibido.getData(), 0, paqueteRecibido.getLength(), COD_TEXTO);
                InetAddress IPCliente = paqueteRecibido.getAddress();
                int puertoCliente = paqueteRecibido.getPort();

                System.out.printf("Recibido datagrama de %s:%d (%s)\n", IPCliente.getHostAddress(), puertoCliente, mensaje);

                // Analizar contenido del mensaje
                String zona, respuesta;
                Pattern patRespuesta = Pattern.compile("@resp#(.+)#(.*)@");
                Matcher m = patRespuesta.matcher(mensaje);

                if (m.find()) {  // Respuesta para la zona
                    zona = m.group(1);
                    respuesta = m.group(2);

                    Thread.sleep(20);
                    resultados.anotaRespuesta(zona, respuesta);
                    Thread.sleep(20);

                } else {  // Fin de resultados para la zona
                    Pattern patFin = Pattern.compile("@fin#(.+)@");
                    m = patFin.matcher(mensaje);
                    if (m.find()) {  // Responder con resultados de la zona
                        zona = m.group(1);
                        String resultEncuesta = "@result#" + zona + "#" + resultados.obtenNumRespuestasZona(zona) + "@";
                        b = resultEncuesta.getBytes(COD_TEXTO);
                        paqueteEnviado = new DatagramPacket(b, b.length,IPCliente, puertoCliente);
                        serverSocket.send(paqueteEnviado);
                    } else if (mensaje.equals("@resultados@")) {
                        String datosResultados = resultados.obtenResultadosXML();
                        b = datosResultados.getBytes(COD_TEXTO);
                        paqueteEnviado = new DatagramPacket(b, b.length, IPCliente, puertoCliente);
                        serverSocket.send(paqueteEnviado);
                    }
                }
            }
        } catch (SocketException ex) {
            System.out.println("Excepción de sockets");
            ex.printStackTrace();
        } catch (IOException ex) {
            System.out.println("Excepción de E/S");
            ex.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}