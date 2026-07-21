/* Ubigeo de Perú para los combos de registro (departamento → provincia → distrito).
   Subconjunto de prototipo centrado en la zona de reparto (Lima Metropolitana) más
   los departamentos principales; se amplía si la tienda abre nuevas zonas. */

export const UBIGEO = {
  "Lima": {
    "Lima": [
      "Ate", "Barranco", "Breña", "Cercado de Lima", "Chorrillos", "Comas",
      "El Agustino", "Independencia", "Jesús María", "La Molina", "La Victoria",
      "Lince", "Los Olivos", "Magdalena del Mar", "Miraflores", "Pueblo Libre",
      "Puente Piedra", "Rímac", "San Borja", "San Isidro", "San Juan de Lurigancho",
      "San Juan de Miraflores", "San Luis", "San Martín de Porres", "San Miguel",
      "Santa Anita", "Santiago de Surco", "Surquillo", "Villa El Salvador",
      "Villa María del Triunfo",
    ],
    "Huaral": ["Huaral", "Chancay", "Aucallama"],
    "Cañete": ["San Vicente de Cañete", "Asia", "Mala", "Cerro Azul"],
  },
  "Callao": {
    "Callao": ["Bellavista", "Callao", "Carmen de la Legua", "La Perla", "La Punta", "Mi Perú", "Ventanilla"],
  },
  "Arequipa": {
    "Arequipa": ["Arequipa", "Cayma", "Cerro Colorado", "José Luis Bustamante y Rivero", "Miraflores", "Paucarpata", "Yanahuara"],
    "Islay": ["Mollendo", "Mejía"],
  },
  "Cusco": {
    "Cusco": ["Cusco", "San Sebastián", "San Jerónimo", "Santiago", "Wanchaq"],
    "Urubamba": ["Urubamba", "Ollantaytambo", "Machupicchu"],
  },
  "La Libertad": {
    "Trujillo": ["Trujillo", "El Porvenir", "Huanchaco", "La Esperanza", "Moche", "Víctor Larco Herrera"],
  },
  "Piura": {
    "Piura": ["Piura", "Castilla", "Catacaos", "Veintiséis de Octubre"],
    "Sullana": ["Sullana", "Bellavista"],
  },
  "Lambayeque": {
    "Chiclayo": ["Chiclayo", "José Leonardo Ortiz", "La Victoria", "Pimentel"],
  },
  "Junín": {
    "Huancayo": ["Huancayo", "El Tambo", "Chilca"],
  },
};
