package com.jad;

import com.jad.connector.DBConnector;
import com.jad.scheduler.JsonPlanGenerator;
import com.jad.scheduler.SmartMonkeyScheduler;
import com.jad.scheduler.model.MachineSchedule;

import java.sql.SQLException;
import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        int idProduct = 200;     // ID du produit à fabriquer
        double quantity = 1000;  // Quantité souhaitée
        String outputFile = "plan_fabrication.json";

        DBConnector db = DBConnector.getInstance();

        SmartMonkeyScheduler scheduler = new SmartMonkeyScheduler(db);
        List<MachineSchedule> plan = scheduler.schedule(idProduct, quantity);

        System.out.println("\n=== PLANNING GÉNÉRÉ ===");
        plan.forEach(ms -> {
            System.out.println("Machine " + ms.getIdMachineTool()
                    + " → " + ms.getOrders().size() + " ordre(s), charge=" + ms.getTotalLoad());
        });

        String json = JsonPlanGenerator.toJson(plan);
        System.out.println("\n=== JSON ===");
        System.out.println(json);

        JsonPlanGenerator.saveToFile(plan, outputFile);

        db.disconnect();
    }
}