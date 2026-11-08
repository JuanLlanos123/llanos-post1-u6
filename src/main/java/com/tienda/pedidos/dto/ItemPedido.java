package com.tienda.pedidos.dto;

public class ItemPedido{
    private Long productoId;
    private int cantidad;
    // getters y setters
      
    public void setProductoId(Long productoId){
        this.productoId=productoId;
    }
    public Long getProductoId(){
        return productoId;
    }
    public void setCantidad(int cantidad){
        this.cantidad=cantidad;
    }
    public int getCantidad(){
        return cantidad;
    }
}