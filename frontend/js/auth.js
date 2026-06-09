import { AuthService } from "./services.js";

const form = document.querySelector("#loginForm");
const error = document.querySelector("#loginError");
const resolveRoute = (file) => new URL(`../pages/${file}`, import.meta.url).href;

const routes = {
  Administrador: resolveRoute("admin.html"),
  Cajero: resolveRoute("cajero.html"),
  Cocina: resolveRoute("cocina.html")
};

form.addEventListener("submit", (event) => {
  event.preventDefault();
  const username = form.username.value.trim();
  const password = form.password.value.trim();
  const user = AuthService.login(username, password);

  if (!user) {
    error.classList.remove("hidden");
    return;
  }

  AuthService.setSession(user);
  window.location.href = routes[user.rol] || new URL("../login.html", import.meta.url).href;
});
