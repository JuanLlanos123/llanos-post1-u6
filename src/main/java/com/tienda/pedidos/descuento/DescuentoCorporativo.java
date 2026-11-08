package com.tienda.pedidos.descuento;

@org.springframework.stereotype.Component
public class DescuentoCorporativo implements EstrategiaDescuento {
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    public DescuentoCorporativo(org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public double calcular(com.tienda.pedidos.validacion.ContextoPedido contexto) {
        String nit = jdbcTemplate.queryForObject(
            "SELECT nit FROM clientes WHERE id = ?", String.class,
            contexto.getRequest().getClienteId());
        return (nit != null && !nit.isBlank()) ? 0.10 : 0.0;
    }
}
