package com.jad.scheduler.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ManufactureOrder {
    private int numOrder;
    private int idProduct;
    private double quantity;
}