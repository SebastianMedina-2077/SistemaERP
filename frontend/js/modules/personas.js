import { actionButtons, appendRows, badge, cell, fillSelect, row } from "../app.js";
import { format, PeopleService } from "../services.js";

export function init(root) {
  renderSelectors(root);
  renderEmployees(root);
  renderUsers(root);
  renderSuppliers(root);
}

function renderSelectors(root) {
  const roles = root.querySelector("#roleOptions");
  const employees = root.querySelector("#employeeOptions");
  if (roles) fillSelect(roles, PeopleService.getRoles(), (item) => item.idRol, (item) => item.nombre);
  if (employees) fillSelect(employees, PeopleService.getEmpleados(), (item) => item.idEmpleado, (item) => `${item.nombre} ${item.apellido}`);
}

function renderEmployees(root) {
  const target = root.querySelector("#employeesTable");
  if (!target) return;
  appendRows(target, PeopleService.getEmpleados().map((item) => row(
    cell(item.idEmpleado),
    cell(`${item.nombre} ${item.apellido}`),
    cell(item.dni),
    cell(item.telefono),
    cell(item.cargo),
    cell(actionButtons())
  )));
}

function renderUsers(root) {
  const target = root.querySelector("#usersTable");
  if (!target) return;
  appendRows(target, PeopleService.getUsuarios().map((item) => {
    const status = item.estado ? "Activo" : "Inactivo";
    return row(
      cell(item.idUsuario),
      cell(item.username),
      cell(`${item.empleado.nombre} ${item.empleado.apellido}`),
      cell(item.rol.nombre),
      cell(badge(status, format.badge(status))),
      cell(actionButtons())
    );
  }));
}

function renderSuppliers(root) {
  const target = root.querySelector("#suppliersTable");
  if (!target) return;
  appendRows(target, PeopleService.getProveedores().map((item) => row(
    cell(item.ruc),
    cell(item.nombre),
    cell(item.telefono),
    cell(item.direccion),
    cell(actionButtons())
  )));
}
