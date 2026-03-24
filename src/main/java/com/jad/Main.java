package com.jad;

import com.jad.connector.DBConnector;
import com.jad.scheduler.JsonPlanGenerator;
import com.jad.scheduler.SmartMonkeyScheduler;
import com.jad.scheduler.model.MachineSchedule;

import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        int idProduct = 200;
        double quantity = 1000;
        String outputFile = "plan_fabrication.json";

        DBConnector db = DBConnector.getInstance();
        SmartMonkeyScheduler scheduler = new SmartMonkeyScheduler(db);
        List<MachineSchedule> plan = scheduler.schedule(idProduct, quantity);

        System.out.println("\n=== PLANNING GÉNÉRÉ ===");
        for (MachineSchedule machinePlan : plan) {
            System.out.println("Machine " + machinePlan.getIdMachineTool()
                    + " → " + machinePlan.getOrders().size() + " ordre(s)"
                    + ", charge=" + machinePlan.getTotalLoad());
        }

        String json = JsonPlanGenerator.toJson(plan);
        System.out.println("\n=== JSON ===");
        System.out.println(json);

        JsonPlanGenerator.saveToFile(plan, outputFile);
        db.disconnect();
    }
}