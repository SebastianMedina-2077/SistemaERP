package com.erp.pizzeria.controller;

import com.erp.pizzeria.dto.ProductoDTO;
import com.erp.pizzeria.dto.StockAlertDTO;
import com.erp.pizzeria.model.Producto;
import com.erp.pizzeria.service.CatalogService;
import com.erp.pizzeria.service.InventarioService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ProductoRestController {

    private final CatalogService catalogService;
    private final InventarioService inventarioService;

    public ProductoRestController(CatalogService catalogService,
                                  InventarioService inventarioService) {
        this.catalogService = catalogService;
        this.inventarioService = inventarioService;
    }

    @GetMapping("/api/productos")
    public List<ProductoDTO> productos(@RequestParam(required = false) Integer categoriaId) {
        List<Producto> productos = (categoriaId == null)
                ? catalogService.listProductosDisponibles()
                : catalogService.listProductosDisponiblesPorCategoria(categoriaId);
        return productos.stream().map(ProductoDTO::from).toList();
    }

    @GetMapping("/api/stock/check")
    public StockAlertDTO checkStock(@RequestParam Integer productoId,
                                    @RequestParam(defaultValue = "1") int cantidad) {
        Producto producto = catalogService.getProducto(productoId);
        return inventarioService.verificarStock(producto, cantidad);
    }
}