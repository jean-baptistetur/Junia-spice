package com.jad.scheduler;

import com.jad.connector.DBConnector;
import com.jad.entity.MachineTool;
import com.jad.entity.OperationType;
import com.jad.entity.Product;
import com.jad.entity.ProductRecipe;
import com.jad.entity.RecipeLine;
import com.jad.scheduler.model.MachineSchedule;
import com.jad.scheduler.model.ManufactureOrder;
import com.jad.service.OperationTypeService;
import com.jad.service.ProductRecipeService;
import com.jad.service.ProductService;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SmartMonkeyScheduler {

    private final ProductService productService;
    private final ProductRecipeService productRecipeService;
    private final OperationTypeService operationTypeService;

    private List<MachineSchedule> planning = new ArrayList<>();
    private List<Integer> alreadyPlanned = new ArrayList<>();

    public SmartMonkeyScheduler(DBConnector dbConnector) throws SQLException {
        this.productService = new ProductService(dbConnector);
        this.productRecipeService = new ProductRecipeService(dbConnector);
        this.operationTypeService = new OperationTypeService(dbConnector);
    }

    public List<MachineSchedule> schedule(int idProduct, double quantity) throws SQLException {
        planning.clear();
        alreadyPlanned.clear();
        planProduct(idProduct, quantity);
        return planning;
    }

    private void planProduct(int idProduct, double quantity) throws SQLException {
        if (alreadyPlanned.contains(idProduct)) {
            return;
        }

        Product product = productService.getById(idProduct);
        if (product == null || Boolean.TRUE.equals(product.getIsAtomic())) {
            return;
        }

        ProductRecipe recipe = productRecipeService.getByIdProduct(idProduct);
        if (recipe == null || recipe.getIdProduct() == null) {
            return;
        }

        OperationType operation = operationTypeService.getById(recipe.getIdOperationType());
        if (operation == null) {
            return;
        }

        double lossRate = operation.getLossOfQuantity() / 100.0;
        double grossQuantity = quantity / (1.0 - lossRate);

        for (RecipeLine component : recipe.getRecipeLines()) {
            double componentQuantity = grossQuantity * (component.getPercentage() / 100.0);
            planProduct(component.getIdComponent(), componentQuantity);
        }

        alreadyPlanned.add(idProduct);

        List<MachineTool> machines = operationTypeService.getMachineToolsForOperationTypeId(recipe.getIdOperationType());
        if (machines.isEmpty()) {
            System.err.println("[Singe] Aucune machine pour : " + operation.getLabel());
            return;
        }

        MachineTool chosenMachine = findLeastLoadedMachine(machines);
        MachineSchedule machinePlan = findOrCreateMachinePlan(chosenMachine.getId());
        int orderNumber = machinePlan.getOrders().size();
        machinePlan.addOrder(new ManufactureOrder(orderNumber, idProduct, grossQuantity));

        System.out.println("[Singe] " + product.getLabel() + " x" + grossQuantity + " → " + chosenMachine.getLabel());
    }

    private MachineTool findLeastLoadedMachine(List<MachineTool> machines) {
        MachineTool bestMachine = machines.get(0);
        double lowestLoad = Double.MAX_VALUE;

        for (MachineTool machine : machines) {
            double currentLoad = getMachineLoad(machine.getId());
            if (currentLoad < lowestLoad) {
                lowestLoad = currentLoad;
                bestMachine = machine;
            }
        }
        return bestMachine;
    }

    private double getMachineLoad(int idMachine) {
        for (MachineSchedule machinePlan : planning) {
            if (machinePlan.getIdMachineTool() == idMachine) {
                return machinePlan.getTotalLoad();
            }
        }
        return 0.0;
    }

    private MachineSchedule findOrCreateMachinePlan(int idMachine) {
        for (MachineSchedule machinePlan : planning) {
            if (machinePlan.getIdMachineTool() == idMachine) {
                return machinePlan;
            }
        }
        MachineSchedule newPlan = new MachineSchedule(idMachine);
        planning.add(newPlan);
        return newPlan;
    }
}