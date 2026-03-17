package com.jad.scheduler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jad.scheduler.model.MachineSchedule;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class JsonPlanGenerator {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static String toJson(List<MachineSchedule> schedules) {
        List<MachineScheduleJson> result = schedules.stream()
                .filter(s -> !s.getOrders().isEmpty())
                .map(s -> new MachineScheduleJson(
                        s.getIdMachineTool(),
                        s.getOrders().stream()
                                .map(o -> new OrderJson(o.getNumOrder(), o.getIdProduct(), o.getQuantity()))
                                .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());
        return GSON.toJson(result);
    }

    public static void saveToFile(List<MachineSchedule> schedules, String filePath) throws IOException {
        String json = toJson(schedules);
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(json);
        }
        System.out.println("Fichier JSON généré : " + filePath);
    }


    private record MachineScheduleJson(int idMachineTool, List<OrderJson> orders) {}
    private record OrderJson(int numOrder, int idProduct, double quantity) {}
}