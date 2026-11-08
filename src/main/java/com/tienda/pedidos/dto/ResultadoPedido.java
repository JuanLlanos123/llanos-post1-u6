package com.tienda.pedidos.dto;

public class ResultadoPedido{
    private boolean confirmado;
    private Long pedidoId;
    private double total;
    private String motivoRechazo;

    public static ResultadoPedido confirmado(Long pedidoId,double total){
        ResultadoPedido r=new ResultadoPedido();
        r.pedidoId=pedidoId;
        r.total=total;
        r.confirmado=true;
        return r;
    }

    public static ResultadoPedido rechazado(String motivo){
        ResultadoPedido r=new ResultadoPedido();
        r.motivoRechazo=motivo;
        r.confirmado=false;
        return r;
    }

    public boolean getConfirmado(){
        return confirmado;
    }
    public Long pedidoId(){
        return pedidoId;
    }
    public double getTotal(){
        return total;
    }
    public String getMotivoRechazo(){
        return motivoRechazo;
    }

}