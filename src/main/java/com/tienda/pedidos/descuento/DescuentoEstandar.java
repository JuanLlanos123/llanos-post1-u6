package com.tienda.pedidos.descuento;

import com.tienda.pedidos.validacion.ContextoPedido;

@org.springframework.stereotype.Component
public class DescuentoEstandar implements EstrategiaDescuento {
    @Override
    public double calcular(ContextoPedido contexto) { return 0.0; }
}