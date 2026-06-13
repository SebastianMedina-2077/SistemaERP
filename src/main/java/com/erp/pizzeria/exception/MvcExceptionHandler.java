package com.erp.pizzeria.exception;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Order(2)
@ControllerAdvice(annotations = Controller.class)
public class MvcExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public String notFound(ResourceNotFoundException ex, Model model, HttpServletResponse response) {
        response.setStatus(HttpStatus.NOT_FOUND.value());
        return errorView(model, "Recurso no encontrado", ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public String badRequest(IllegalArgumentException ex, Model model, HttpServletResponse response) {
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        return errorView(model, "Operacion no valida", ex.getMessage());
    }

    @ExceptionHandler(StockInsuficienteException.class)
    public String stock(StockInsuficienteException ex, Model model, HttpServletResponse response) {
        response.setStatus(HttpStatus.CONFLICT.value());
        return errorView(model, "Stock insuficiente", ex.getMessage());
    }

    private String errorView(Model model, String titulo, String detalle) {
        model.addAttribute("errorTitulo", titulo);
        model.addAttribute("errorDetalle", detalle);
        return "error";
    }
}
