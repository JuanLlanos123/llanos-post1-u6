package com.tienda.pedidos.descuento;

// El descuento final combina la estrategia por tipo de cliente con las campañas activas,
// tomando el mayor -- la misma regla de negocio que antes, sin escribir en un campo
// mutable compartido desde clases que no son responsables de validar nada
@org.springframework.stereotype.Component
public class CalculadorDescuentoFinal {
    private final SelectorEstrategiaDescuento selectorPorCliente;
    private final java.util.List<EstrategiaDescuento> campanas;

    public CalculadorDescuentoFinal(SelectorEstrategiaDescuento selectorPorCliente,
                                     DescuentoBlackFriday blackFriday, DescuentoCorporativo corporativo,
                                     DescuentoVolumen volumen) {
        this.selectorPorCliente = selectorPorCliente;
        this.campanas = java.util.List.of(blackFriday, corporativo, volumen);
    }

    public double calcular(com.tienda.pedidos.validacion.ContextoPedido contexto) {
        double porTipoCliente = selectorPorCliente.seleccionar(contexto.getTipoCliente()).calcular(contexto);
        double porCampana = campanas.stream()
            .mapToDouble(estrategia -> estrategia.calcular(contexto))
            .max().orElse(0.0);
        return Math.max(porTipoCliente, porCampana);
    }
}