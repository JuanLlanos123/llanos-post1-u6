package com.tienda.pedidos.service;
import com.tienda.pedidos.validacion.ContextoPedido;

@org.springframework.stereotype.Repository
public class PedidoRepository {
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    public PedidoRepository(org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long guardar(ContextoPedido contexto, double descuento, double impuesto, double total) {
        jdbcTemplate.update(
            "INSERT INTO pedidos (cliente_id, subtotal, descuento, impuesto, total, fecha, estado) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)",
            contexto.getRequest().getClienteId(), contexto.getSubtotal(), descuento, impuesto, total,
            java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()), "CONFIRMADO");
        Long pedidoId = jdbcTemplate.queryForObject("CALL IDENTITY()", Long.class);
        for (var item : contexto.getRequest().getItems()) {
            jdbcTemplate.update(
                "INSERT INTO detalle_pedido (pedido_id, producto_id, cantidad) VALUES (?, ?, ?)",
                pedidoId, item.getProductoId(), item.getCantidad());
            jdbcTemplate.update("UPDATE inventario SET stock = stock - ? WHERE producto_id = ?",
                item.getCantidad(), item.getProductoId());
        }
        return pedidoId;
    }
}