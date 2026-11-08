package com.tienda.pedidos.service;

import com.tienda.pedidos.descuento.CalculadorDescuentoFinal;
import com.tienda.pedidos.descuento.DescuentoBlackFriday;
import com.tienda.pedidos.descuento.DescuentoCorporativo;
import com.tienda.pedidos.descuento.DescuentoEstandar;
import com.tienda.pedidos.descuento.DescuentoFrecuente;
import com.tienda.pedidos.descuento.DescuentoVolumen;
import com.tienda.pedidos.descuento.DescuentoVip;
import com.tienda.pedidos.descuento.SelectorEstrategiaDescuento;
import com.tienda.pedidos.dto.ItemPedido;
import com.tienda.pedidos.dto.PedidoRequest;
import com.tienda.pedidos.dto.ResultadoPedido;
import com.tienda.pedidos.validacion.ContextoPedido;
import com.tienda.pedidos.validacion.ValidadorCliente;
import com.tienda.pedidos.validacion.ValidadorStock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GestorPedidosTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private PedidoRepository repository;

    @Mock
    private NotificacionPedidoService notificacion;

    private SelectorEstrategiaDescuento selector;

    @BeforeEach
    void configurarSelector() {
        selector = new SelectorEstrategiaDescuento(
                new DescuentoVip(),
                new DescuentoFrecuente(jdbcTemplate),
                new DescuentoEstandar());
    }

    @Test
    void rechazaPedidoSinStockYDetieneLaCadena() {
        when(jdbcTemplate.queryForObject(
                eq("SELECT stock FROM inventario WHERE producto_id = ?"),
                eq(Integer.class),
                any(Object[].class))).thenReturn(1);

        GestorPedidos gestor = crearGestor(true);
        PedidoRequest request = crearPedido(1L, 2, 100L);

        ResultadoPedido resultado = gestor.procesarPedido(request);

        assertFalse(resultado.getConfirmado());
        assertEquals("Stock insuficiente: producto 100", resultado.getMotivoRechazo());
        verify(jdbcTemplate, never()).queryForObject(
                eq("SELECT tipo_cliente FROM clientes WHERE id = ?"),
                eq(String.class),
                any(Object[].class));
        verify(repository, never()).guardar(
                any(ContextoPedido.class), eq(0.0), eq(0.0), eq(0.0));
        verify(notificacion, never()).notificarConfirmacion(
                any(ContextoPedido.class), any(), eq(0.0), eq(0.0), eq(0.0));
    }

    @Test
    void rechazaClienteNoRegistradoAntesDeEvaluarPromociones() {
        prepararStockDisponible();
        when(jdbcTemplate.queryForObject(
                eq("SELECT tipo_cliente FROM clientes WHERE id = ?"),
                eq(String.class),
                any(Object[].class))).thenReturn(null);

        GestorPedidos gestor = crearGestor(true);
        ResultadoPedido resultado = gestor.procesarPedido(crearPedido(9L, 1, 100L));

        assertFalse(resultado.getConfirmado());
        assertEquals("Cliente no registrado", resultado.getMotivoRechazo());
        verify(jdbcTemplate, never()).queryForObject(
                eq("SELECT nit FROM clientes WHERE id = ?"),
                eq(String.class),
                any(Object[].class));
        verify(repository, never()).guardar(
                any(ContextoPedido.class), eq(0.0), eq(0.0), eq(0.0));
    }

    @Test
    void blackFridayActivaConservaElDescuentoYTotalDeLaCadenaAnterior() {
        prepararPedidoValido("ESTANDAR", null);
        when(repository.guardar(any(ContextoPedido.class), eq(0.25), eq(14.25), eq(89.25)))
                .thenReturn(42L);
        GestorPedidos gestor = crearGestor(true);
        PedidoRequest request = crearPedido(1L, 1, 100L);

        ResultadoPedido resultado = gestor.procesarPedido(request);

        assertTrue(resultado.getConfirmado());
        assertEquals(42L, resultado.pedidoId());
        assertEquals(89.25, resultado.getTotal(), 0.001);
        ArgumentCaptor<ContextoPedido> contextoCaptor = ArgumentCaptor.forClass(ContextoPedido.class);
        verify(repository).guardar(
                contextoCaptor.capture(), eq(0.25), eq(14.25), eq(89.25));
        assertEquals(100.0, contextoCaptor.getValue().getSubtotal(), 0.001);
        assertEquals("ESTANDAR", contextoCaptor.getValue().getTipoCliente());
        verify(notificacion).notificarConfirmacion(
                eq(contextoCaptor.getValue()), eq(42L), eq(0.25), eq(14.25), eq(89.25));
    }

    @Test
    void clienteConNitConservaElDescuentoYTotalDeLaCadenaAnterior() {
        prepararPedidoValido("ESTANDAR", "NIT-123");
        when(repository.guardar(any(ContextoPedido.class), eq(0.10), eq(17.1), eq(107.1)))
                .thenReturn(43L);
        GestorPedidos gestor = crearGestor(false);
        PedidoRequest request = crearPedido(2L, 1, 100L);

        ResultadoPedido resultado = gestor.procesarPedido(request);

        assertTrue(resultado.getConfirmado());
        assertEquals(43L, resultado.pedidoId());
        assertEquals(107.1, resultado.getTotal(), 0.001);
        verify(repository).guardar(
                any(ContextoPedido.class), eq(0.10), eq(17.1), eq(107.1));
        verify(jdbcTemplate).queryForObject(
                eq("SELECT nit FROM clientes WHERE id = ?"),
                eq(String.class),
                any(Object[].class));
    }

    @Test
    void pedidoConMasDeVeinteUnidadesConservaElDescuentoYTotalDeLaCadenaAnterior() {
        prepararPedidoValido("ESTANDAR", null);
        when(repository.guardar(any(ContextoPedido.class), eq(0.12), eq(351.12), eq(2_199.12)))
                .thenReturn(44L);
        GestorPedidos gestor = crearGestor(false);
        PedidoRequest request = crearPedido(2L, 21, 100L);

        ResultadoPedido resultado = gestor.procesarPedido(request);

        assertTrue(resultado.getConfirmado());
        assertEquals(44L, resultado.pedidoId());
        assertEquals(2_199.12, resultado.getTotal(), 0.001);
        verify(repository).guardar(
                any(ContextoPedido.class), eq(0.12), eq(351.12), eq(2_199.12));
        verify(jdbcTemplate).queryForObject(
                eq("SELECT nit FROM clientes WHERE id = ?"),
                eq(String.class),
                any(Object[].class));
    }

    private GestorPedidos crearGestor(boolean blackFridayActiva) {
        CalculadorDescuentoFinal calculador = new CalculadorDescuentoFinal(
                selector,
                new DescuentoBlackFriday(blackFridayActiva),
                new DescuentoCorporativo(jdbcTemplate),
                new DescuentoVolumen());
        return new GestorPedidos(
                new ValidadorStock(jdbcTemplate),
                new ValidadorCliente(jdbcTemplate),
                calculador,
                repository,
                notificacion);
    }

    private void prepararStockDisponible() {
        when(jdbcTemplate.queryForObject(
                eq("SELECT stock FROM inventario WHERE producto_id = ?"),
                eq(Integer.class),
                any(Object[].class))).thenReturn(100);
    }

    private void prepararPedidoValido(String tipoCliente, String nit) {
        prepararStockDisponible();
        when(jdbcTemplate.queryForObject(
                eq("SELECT tipo_cliente FROM clientes WHERE id = ?"),
                eq(String.class),
                any(Object[].class))).thenReturn(tipoCliente);
        when(jdbcTemplate.queryForObject(
                eq("SELECT nit FROM clientes WHERE id = ?"),
                eq(String.class),
                any(Object[].class))).thenReturn(nit);
    }

    private PedidoRequest crearPedido(long clienteId, int cantidad, long precio) {
        ItemPedido item = new ItemPedido();
        item.setProductoId(precio);
        item.setCantidad(cantidad);

        PedidoRequest request = new PedidoRequest();
        request.setClienteId(clienteId);
        request.setClienteEmail("cliente@tienda.com");
        request.setItems(List.of(item));
        return request;
    }
}
