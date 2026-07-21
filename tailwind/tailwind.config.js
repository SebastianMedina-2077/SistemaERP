/**
 * Config de Tailwind para la tienda web (uso puntual en paginas nuevas).
 * Se compila con el binario standalone (sin Node) a static/css/tailwind.tienda.css.
 * preflight desactivado para NO pisar el CSS de tokens/vanilla existente.
 * Comando de regeneracion en doc/manual-del-programador.md.
 */
module.exports = {
  content: [
    "./src/main/resources/templates/tienda/**/*.html",
    "./src/main/resources/static/js/tienda*.js",
  ],
  corePlugins: { preflight: false },
  theme: {
    extend: {
      colors: {
        verde: "#217A3A",
        "verde-banner": "#267440",
        "verde-profundo": "#185C2C",
        "verde-tinte": "#E7F3EC",
        tomate: "#C0392B",
        "tomate-oscuro": "#8E2820",
        crema: "#FAF3E7",
        "crema-oscura": "#F0E4D0",
        carbon: "#2B2119",
        "carbon-suave": "#5C5148",
        horno: "#F5A623",
      },
      fontFamily: {
        display: ['"Alfa Slab One"', "cursive"],
        cuerpo: ['"Rubik"', "system-ui", "sans-serif"],
      },
    },
  },
  plugins: [],
};
