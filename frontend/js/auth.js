import { AuthService } from "./services.js";

const form = document.querySelector("#loginForm");
const error = document.querySelector("#loginError");

const routes = {
  Administrador: "admin.html",
  Cajero: "cajero.html",
  Cocina: "cocina.html"
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
  window.location.href = routes[user.rol] || "login.html";
});
