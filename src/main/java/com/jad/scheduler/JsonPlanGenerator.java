package com.jad.scheduler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jad.scheduler.model.MachineSchedule;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class JsonPlanGenerator {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static String toJson(List<MachineSchedule> plan) {
        return GSON.toJson(plan);
    }

    public static void saveToFile(List<MachineSchedule> plan, String filePath) throws IOException {
        String json = toJson(plan);
        FileWriter writer = new FileWriter(filePath);
        writer.write(json);
        writer.close();
        System.out.println("Fichier JSON généré : " + filePath);
    }
}