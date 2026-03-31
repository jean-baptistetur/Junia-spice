package com.jad.scheduler.model;

import lombok.Getter;
import java.util.ArrayList;
import java.util.List;

@Getter
public class MachineSchedule {
    private final int idMachineTool;
    private final List<ManufactureOrder> orders = new ArrayList<>();

    public MachineSchedule(int idMachineTool) {
        this.idMachineTool = idMachineTool;
    }

    public void addOrder(ManufactureOrder order) {
        this.orders.add(order);
    }

    public double getTotalLoad() {
        return orders.size();
    }
}