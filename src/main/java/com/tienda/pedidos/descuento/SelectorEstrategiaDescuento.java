package com.tienda.pedidos.descuento;
// Selector -- un unico punto de decision, reemplaza el if/else anidado por tipo
@org.springframework.stereotype.Component
public class SelectorEstrategiaDescuento {
    private final java.util.Map<String, EstrategiaDescuento> estrategias;

    public SelectorEstrategiaDescuento(DescuentoVip vip, DescuentoFrecuente frecuente,
                                        DescuentoEstandar estandar) {
        this.estrategias = java.util.Map.of("VIP", vip, "FRECUENTE", frecuente, "ESTANDAR", estandar);
    }

    public EstrategiaDescuento seleccionar(String tipoCliente) {
        return estrategias.getOrDefault(tipoCliente, estrategias.get("ESTANDAR"));
    }
}