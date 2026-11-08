package com.tienda.pedidos.service;

import org.springframework.stereotype.Service;

@Service
public class EmailServiceConsola implements EmailService {

    @Override
    public void enviar(String destinatario, String asunto, String cuerpo) {

        System.out.println("\n========== CORREO SIMULADO ==========");
        System.out.println("Para: " + destinatario);
        System.out.println("Asunto: " + asunto);
        System.out.println("-------------------------------------");
        System.out.println(cuerpo);
        System.out.println("=====================================\n");
    }
}