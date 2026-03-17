package com.jad.scheduler;

import com.jad.connector.DBConnector;
import com.jad.entity.MachineTool;
import com.jad.entity.OperationType;
import com.jad.entity.Product;
import com.jad.entity.ProductRecipe;
import com.jad.entity.RecipeLine;
import com.jad.scheduler.model.MachineSchedule;
import com.jad.scheduler.model.ManufactureOrder;
import com.jad.service.MachineToolService;
import com.jad.service.OperationTypeService;
import com.jad.service.ProductRecipeService;
import com.jad.service.ProductService;

import java.sql.SQLException;
import java.util.*;

public class SmartMonkeyScheduler {

    private final ProductService productService;
    private final ProductRecipeService productRecipeService;
    private final OperationTypeService operationTypeService;
    private final MachineToolService machineToolService;

    private final Map<Integer, MachineSchedule> scheduleMap = new HashMap<>();
    private final Map<Integer, Double> quantityMemo = new HashMap<>();
    private int orderCounter = 0;

    public SmartMonkeyScheduler(DBConnector dbConnector) throws SQLException {
        this.productService = new ProductService(dbConnector);
        this.productRecipeService = new ProductRecipeService(dbConnector);
        this.operationTypeService = new OperationTypeService(dbConnector);
        this.machineToolService = new MachineToolService(dbConnector);
    }

    public List<MachineSchedule> schedule(int idProduct, double quantity) throws SQLException {
        scheduleMap.clear();
        quantityMemo.clear();
        orderCounter = 0;

        accumulateQuantities(idProduct, quantity);
        Set<Integer> planned = new HashSet<>();
        planInOrder(idProduct, planned);

        return new ArrayList<>(scheduleMap.values());
    }

    private void accumulateQuantities(int idProduct, double quantity) throws SQLException {
        Product product = productService.getById(idProduct);
        if (product == null || Boolean.TRUE.equals(product.getIsAtomic())) return;

        ProductRecipe recipe = productRecipeService.getByIdProduct(idProduct);
        if (recipe == null || recipe.getIdProduct() == null) return;

        OperationType operationType = operationTypeService.getById(recipe.getIdOperationType());
        if (operationType == null) return;

        double lossRate = operationType.getLossOfQuantity() / 100.0;
        double grossQuantity = quantity / (1.0 - lossRate);

        quantityMemo.merge(idProduct, grossQuantity, Double::sum);

        for (RecipeLine line : recipe.getRecipeLines()) {
            double componentQty = grossQuantity * (line.getPercentage() / 100.0);
            accumulateQuantities(line.getIdComponent(), componentQty);
        }
    }

    private void planInOrder(int idProduct, Set<Integer> planned) throws SQLException {
        if (planned.contains(idProduct)) return;

        Product product = productService.getById(idProduct);
        if (product == null || Boolean.TRUE.equals(product.getIsAtomic())) {
            planned.add(idProduct);
            return;
        }

        ProductRecipe recipe = productRecipeService.getByIdProduct(idProduct);
        if (recipe == null || recipe.getIdProduct() == null) {
            planned.add(idProduct);
            return;
        }

        for (RecipeLine line : recipe.getRecipeLines()) {
            planInOrder(line.getIdComponent(), planned);
        }

        planned.add(idProduct);

        OperationType operationType = operationTypeService.getById(recipe.getIdOperationType());
        if (operationType == null) return;

        double totalQuantity = quantityMemo.getOrDefault(idProduct, 0.0);
        if (totalQuantity <= 0) return;

        List<MachineTool> machines = operationTypeService.getMachineToolsForOperationTypeId(recipe.getIdOperationType());
        if (machines.isEmpty()) {
            System.err.println("[Singe] Aucune machine pour : " + operationType.getLabel());
            return;
        }

        int nbComponents = recipe.getRecipeLines().size();
        if (nbComponents < operationType.getMinNbComponents() || nbComponents > operationType.getMaxNbComponents()) {
            System.err.println("[Singe] Nombre de composants invalide pour : " + operationType.getLabel()
                    + " (attendu: " + operationType.getMinNbComponents()
                    + "-" + operationType.getMaxNbComponents()
                    + ", trouvé: " + nbComponents + ")");
            return;
        }
        double remaining = totalQuantity;
        while (remaining > 0) {
            MachineTool chosen = chooseLeastLoadedMachine(machines);

            double lotQty = Math.min(remaining, chosen.getMaxQuantity());
            lotQty = Math.max(lotQty, chosen.getMinQuantity());
            remaining -= lotQty;

            MachineSchedule machineSchedule = scheduleMap.computeIfAbsent(chosen.getId(), MachineSchedule::new);
            machineSchedule.addOrder(new ManufactureOrder(orderCounter++, idProduct, lotQty));

            System.out.println("[Singe] Planifié : " + product.getLabel()
                    + " x" + lotQty + " → " + chosen.getLabel());
        }
    }

    private MachineTool chooseLeastLoadedMachine(List<MachineTool> machines) {
        return machines.stream()
                .min(Comparator.comparingDouble(m -> {
                    MachineSchedule ms = scheduleMap.get(m.getId());
                    return ms == null ? 0.0 : ms.getTotalLoad();
                }))
                .orElse(machines.get(0));
    }
}