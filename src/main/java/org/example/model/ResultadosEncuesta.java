package org.example.model;

import java.util.HashMap;
import java.util.Set;

public class ResultadosEncuesta {

    private final HashMap<String, Integer> totalPorRespuesta = new HashMap<>();
    private final HashMap<String, Integer> totalPorZona = new HashMap<>();

    // Suma uno a número de respuestas para la zona y para la respuesta
    synchronized public void anotaRespuesta(String idZona, String respuesta) {
        Integer numRespValor = this.totalPorRespuesta.get(respuesta);
        this.totalPorRespuesta.put(respuesta, numRespValor == null ? 1 : numRespValor + 1);
        Integer numRespZona = this.totalPorZona.get(idZona);
        this.totalPorZona.put(idZona, numRespZona == null ? 1 : numRespZona + 1);
    }
    synchronized public Set<String> obtenZonas() {
        return this.totalPorZona.keySet();
    }
    synchronized public Set<String> obtenRespuestas() {
        return this.totalPorRespuesta.keySet();
    }
    synchronized public int obtenNumRespuestasZona(String zona) {
        return this.totalPorZona.get(zona);
    }
    synchronized public int obtenNumRespuestas(String respuesta) {
        return this.totalPorRespuesta.get(respuesta);
    }

    synchronized public String obtenResultadosXML() {
        String result = "<resultados><porzonas>";
        Set<String> zonas = this.obtenZonas();
        int granTotalPorZonas = 0;
        for (String unaZona : zonas) {
            int totalParaZona = this.obtenNumRespuestasZona(unaZona);
            result += "<zona id=\"" + unaZona + "\">" + totalParaZona + "</zona>";
            granTotalPorZonas += totalParaZona;
        }

        result += "</porzonas><grantotalzonas>" + granTotalPorZonas + "</grantotalzonas>" + "<porrespuestas>";

        Set<String> respuestas = this.obtenRespuestas();
        int granTotalPorRespuestas = 0;
        for (String unaRespuesta : respuestas) {
            int totalParaRespuesta = this.obtenNumRespuestas(unaRespuesta);
            result += "<respuesta valor=\"" + (!unaRespuesta.equals("") ? unaRespuesta : "NS/NC") + "\">" + totalParaRespuesta + "</respuesta>";
            granTotalPorRespuestas += totalParaRespuesta;
        }
        result += "</porrespuestas><grantotalrespuestas>" + granTotalPorRespuestas + "</grantotalrespuestas>" + "</resultados>";

        return result;
    }
}