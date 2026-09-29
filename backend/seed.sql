-- Datos de demostración para presentarle al profesor sin tener que
-- registrar cuentas a mano. Todos los usuarios tienen la contraseña: demo123

INSERT INTO usuarios (nombre, email, password_hash) VALUES
  ('Luis (tú)',      'luis@demo.com',     '$2b$10$3BXiGVkGsbC6290oBVMcDuWyX4guTNMrHcyAprr86IX/1IfxDnqVe'),
  ('Camila Rosero',  'camila@demo.com',   '$2b$10$3BXiGVkGsbC6290oBVMcDuWyX4guTNMrHcyAprr86IX/1IfxDnqVe'),
  ('Andrés Bravo',   'andres@demo.com',   '$2b$10$3BXiGVkGsbC6290oBVMcDuWyX4guTNMrHcyAprr86IX/1IfxDnqVe'),
  ('Valentina Paz',  'valentina@demo.com','$2b$10$3BXiGVkGsbC6290oBVMcDuWyX4guTNMrHcyAprr86IX/1IfxDnqVe'),
  ('Juan Erazo',     'juan@demo.com',     '$2b$10$3BXiGVkGsbC6290oBVMcDuWyX4guTNMrHcyAprr86IX/1IfxDnqVe'),
  ('Sofía Muñoz',    'sofia@demo.com',    '$2b$10$3BXiGVkGsbC6290oBVMcDuWyX4guTNMrHcyAprr86IX/1IfxDnqVe')
ON CONFLICT (email) DO NOTHING;

INSERT INTO perfiles (usuario_id, presupuesto, zona, horario, limpieza, tolerancia_ruido, frecuencia_visitas, tiene_mascotas, acepta_mascotas, descripcion)
SELECT id, 600000, 'Centro, Pasto', 'mixto', 4, 2, 'ocasional', false, true,
       'Estudiante de séptimo semestre, busco un ambiente tranquilo para estudiar.'
FROM usuarios WHERE email = 'luis@demo.com'
ON CONFLICT (usuario_id) DO NOTHING;

INSERT INTO perfiles (usuario_id, presupuesto, zona, horario, limpieza, tolerancia_ruido, frecuencia_visitas, tiene_mascotas, acepta_mascotas, descripcion)
SELECT id, 650000, 'Centro, Pasto', 'mixto', 5, 1, 'ocasional', false, true,
       'Muy ordenada, trabajo desde casa y necesito silencio en las mañanas.'
FROM usuarios WHERE email = 'camila@demo.com'
ON CONFLICT (usuario_id) DO NOTHING;

INSERT INTO perfiles (usuario_id, presupuesto, zona, horario, limpieza, tolerancia_ruido, frecuencia_visitas, tiene_mascotas, acepta_mascotas, descripcion)
SELECT id, 550000, 'Chapal, Pasto', 'nocturno', 2, 5, 'frecuente', true, true,
       'Me gusta recibir amigos, tengo un gato, trabajo de noche.'
FROM usuarios WHERE email = 'andres@demo.com'
ON CONFLICT (usuario_id) DO NOTHING;

INSERT INTO perfiles (usuario_id, presupuesto, zona, horario, limpieza, tolerancia_ruido, frecuencia_visitas, tiene_mascotas, acepta_mascotas, descripcion)
SELECT id, 700000, 'Centro, Pasto', 'madrugador', 4, 2, 'nunca', false, false,
       'Prefiero vivir con alguien tranquilo, sin mascotas, rutina de madrugar.'
FROM usuarios WHERE email = 'valentina@demo.com'
ON CONFLICT (usuario_id) DO NOTHING;

INSERT INTO perfiles (usuario_id, presupuesto, zona, horario, limpieza, tolerancia_ruido, frecuencia_visitas, tiene_mascotas, acepta_mascotas, descripcion)
SELECT id, 500000, 'Universidad, Pasto', 'mixto', 3, 3, 'ocasional', false, true,
       'Nada estricto con el orden, busco algo económico cerca a la universidad.'
FROM usuarios WHERE email = 'juan@demo.com'
ON CONFLICT (usuario_id) DO NOTHING;

INSERT INTO perfiles (usuario_id, presupuesto, zona, horario, limpieza, tolerancia_ruido, frecuencia_visitas, tiene_mascotas, acepta_mascotas, descripcion)
SELECT id, 620000, 'Centro, Pasto', 'mixto', 4, 2, 'ocasional', false, true,
       'Horarios de universidad, me gusta un ambiente limpio y tranquilo.'
FROM usuarios WHERE email = 'sofia@demo.com'
ON CONFLICT (usuario_id) DO NOTHING;
