package com.tienda.pedidos.validacion;
// Eslabon 1: existencia de stock (un solo motivo de rechazo, una sola responsabilidad)
@org.springframework.stereotype.Component
public class ValidadorStock extends ValidadorPedido {
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    public ValidadorStock(org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    protected void ejecutar(ContextoPedido contexto) {
        for (var item : contexto.getRequest().getItems()) {
            Integer stock = jdbcTemplate.queryForObject(
                "SELECT stock FROM inventario WHERE producto_id = ?", Integer.class, item.getProductoId());
            if (stock == null || stock < item.getCantidad()) {
                contexto.rechazar("Stock insuficiente: producto " + item.getProductoId());
                return;
            }
        }
    }
}