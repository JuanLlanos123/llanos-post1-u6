package com.tienda.pedidos.descuento;

@org.springframework.stereotype.Component
public class DescuentoVolumen implements EstrategiaDescuento {
    @Override
    public double calcular(com.tienda.pedidos.validacion.ContextoPedido contexto) {
        int totalUnidades = contexto.getRequest().getItems().stream()
            .mapToInt(item -> item.getCantidad()).sum();
        return totalUnidades > 20 ? 0.12 : 0.0;
    }
}
