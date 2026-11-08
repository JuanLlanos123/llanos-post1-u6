package com.tienda.pedidos.service;
import java.util.Map;
import com.tienda.pedidos.descuento.SelectorEstrategiaDescuento;
import com.tienda.pedidos.dto.ItemPedido;
import com.tienda.pedidos.dto.PedidoRequest;
import com.tienda.pedidos.dto.ResultadoPedido;
import com.tienda.pedidos.validacion.ContextoPedido;
import com.tienda.pedidos.validacion.ValidadorCliente;
import com.tienda.pedidos.validacion.ValidadorPedido;
import com.tienda.pedidos.validacion.ValidadorStock;


@org.springframework.stereotype.Service
public class GestorPedidos {
    private final ValidadorPedido primerValidador;
    private final SelectorEstrategiaDescuento selector;
    private final PedidoRepository repository;
    private final NotificacionPedidoService notificacion;

    public GestorPedidos(ValidadorStock stock, ValidadorCliente cliente,
                          SelectorEstrategiaDescuento selector, PedidoRepository repository,
                          NotificacionPedidoService notificacion) {
        this.primerValidador = stock.encadenar(cliente);
        this.selector = selector;
        this.repository = repository;
        this.notificacion = notificacion;
    }

    public ResultadoPedido procesarPedido(PedidoRequest request) {
        ContextoPedido contexto = new ContextoPedido(request);
        primerValidador.validar(contexto);
        if (contexto.isRechazado()) return ResultadoPedido.rechazado(contexto.getMotivoRechazo());

        double subtotal = calcularSubtotal(request); // consulta de precios extraida sin cambios de logica
        contexto.setSubtotal(subtotal);

        double descuento = selector.seleccionar(contexto.getTipoCliente()).calcular(contexto);
        double impuesto = (subtotal - subtotal * descuento) * 0.19;
        double total = subtotal - (subtotal * descuento) + impuesto;

        Long pedidoId = repository.guardar(contexto, descuento, impuesto, total);
        notificacion.notificarConfirmacion(contexto, pedidoId, descuento, impuesto, total);
        return ResultadoPedido.confirmado(pedidoId, total);
    }

    private double calcularSubtotal(PedidoRequest request) {
        double subtotal = 0;

    for (ItemPedido item : request.getItems()) {
        double precio = item.getProductoId();
        subtotal += precio * item.getCantidad();
    }

        return subtotal;
    }


    // calcularSubtotal(...) se mantiene como metodo privado de calculo puro, sin SQL embebido
    // en la logica de negocio (se extrae a un pequeno metodo con una unica consulta por item)
}

