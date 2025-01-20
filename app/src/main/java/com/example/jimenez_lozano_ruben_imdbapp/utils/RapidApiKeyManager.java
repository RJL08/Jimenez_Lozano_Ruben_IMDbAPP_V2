package com.example.jimenez_lozano_ruben_imdbapp.utils;

import java.util.ArrayList;
import java.util.List;

public class RapidApiKeyManager {

    private final List<String> apiKeys = new ArrayList<>();
    private int currentKeyIndex = 0;

    public RapidApiKeyManager() {
        // Añade tus claves de RapidAPI
        apiKeys.add("3ef3f2c2a3msh17da27eb24608e1p12db6bjsn62d2b74752ff");
        apiKeys.add("8c8a3cbdefmsh5b39dc7ade88a71p1ca1bdjsn245a12339ee4");
        apiKeys.add("cb2c7cc95cmsh29ce53fe16403a4p10aee0jsn342403956b33");
    }

    /**
     * Obtiene la clave de API actual.
     *
     * @return clave de API actual
     */
    public String getCurrentKey() {
        return apiKeys.get(currentKeyIndex);
    }

    /**
     * Cambia a la siguiente clave de API en la lista.
     */
    public void switchToNextKey() {
        currentKeyIndex = (currentKeyIndex + 1) % apiKeys.size();
    }
}
