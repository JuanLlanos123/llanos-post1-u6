package com.tienda.pedidos.validacion;
// Eslabon 2: existencia y mora del cliente (depende de que el eslabon anterior haya pasado)
@org.springframework.stereotype.Component
public class ValidadorCliente extends ValidadorPedido {
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    public ValidadorCliente(org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    protected void ejecutar(ContextoPedido contexto) {
        Long clienteId = contexto.getRequest().getClienteId();
        String tipo = jdbcTemplate.queryForObject(
            "SELECT tipo_cliente FROM clientes WHERE id = ?", String.class, clienteId);
        if (tipo == null) { contexto.rechazar("Cliente no registrado"); return; }
        contexto.setTipoCliente(tipo);

        if (tipo.equals("MOROSO")) {
            Double deuda = jdbcTemplate.queryForObject(
                "SELECT SUM(monto) FROM facturas WHERE cliente_id = ? AND pagada = false",
                Double.class, clienteId);
            boolean fueraDeHorarioDeCorte = !java.time.LocalTime.now().isBefore(java.time.LocalTime.of(20, 0));
            if (deuda != null && deuda > 0 && !fueraDeHorarioDeCorte) {
                contexto.rechazar("Cliente con deuda pendiente: $" + deuda);
            }
        }
    }
}