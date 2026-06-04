from docx import Document
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_CELL_VERTICAL_ALIGNMENT
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Inches, Pt, RGBColor


OUTPUT = "docs/ActiHome_Guia_Tecnica_Estudio.docx"


def set_cell_shading(cell, fill):
    tc_pr = cell._tc.get_or_add_tcPr()
    shd = OxmlElement("w:shd")
    shd.set(qn("w:fill"), fill)
    tc_pr.append(shd)


def set_cell_text(cell, text, bold=False):
    cell.text = ""
    paragraph = cell.paragraphs[0]
    run = paragraph.add_run(text)
    run.bold = bold
    run.font.name = "Arial"
    run.font.size = Pt(9)
    cell.vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.CENTER


def add_code(doc, code):
    table = doc.add_table(rows=1, cols=1)
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    table.autofit = True
    cell = table.cell(0, 0)
    set_cell_shading(cell, "F3F6F8")
    paragraph = cell.paragraphs[0]
    paragraph.paragraph_format.space_after = Pt(0)
    for line_no, line in enumerate(code.strip("\n").split("\n")):
        if line_no:
            paragraph.add_run("\n")
        run = paragraph.add_run(line)
        run.font.name = "Consolas"
        run.font.size = Pt(8.5)
    doc.add_paragraph()


def add_note(doc, title, body):
    table = doc.add_table(rows=1, cols=1)
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    cell = table.cell(0, 0)
    set_cell_shading(cell, "FFF7E6")
    p = cell.paragraphs[0]
    r = p.add_run(title + ": ")
    r.bold = True
    r.font.name = "Arial"
    r.font.size = Pt(10)
    r.font.color.rgb = RGBColor(120, 72, 0)
    r2 = p.add_run(body)
    r2.font.name = "Arial"
    r2.font.size = Pt(10)
    doc.add_paragraph()


def add_table(doc, headers, rows):
    table = doc.add_table(rows=1, cols=len(headers))
    table.style = "Table Grid"
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    for i, header in enumerate(headers):
        cell = table.rows[0].cells[i]
        set_cell_shading(cell, "DDEBF7")
        set_cell_text(cell, header, bold=True)
    for row in rows:
        cells = table.add_row().cells
        for i, value in enumerate(row):
            set_cell_text(cells[i], value)
    doc.add_paragraph()


def configure_styles(doc):
    styles = doc.styles
    normal = styles["Normal"]
    normal.font.name = "Arial"
    normal.font.size = Pt(10.5)
    normal.paragraph_format.space_after = Pt(6)
    normal.paragraph_format.line_spacing = 1.08

    for style_name, size, color in [
        ("Title", 24, "1F4E79"),
        ("Heading 1", 16, "1F4E79"),
        ("Heading 2", 13, "2F5597"),
        ("Heading 3", 11, "404040"),
    ]:
        style = styles[style_name]
        style.font.name = "Arial"
        style.font.size = Pt(size)
        style.font.bold = True
        style.font.color.rgb = RGBColor.from_string(color)


def add_page_number(section):
    footer = section.footer
    p = footer.paragraphs[0]
    p.alignment = WD_ALIGN_PARAGRAPH.RIGHT
    run = p.add_run("ActiHome - Guia tecnica")
    run.font.name = "Arial"
    run.font.size = Pt(8)
    run.font.color.rgb = RGBColor(120, 120, 120)


def build():
    doc = Document()
    section = doc.sections[0]
    section.top_margin = Inches(0.75)
    section.bottom_margin = Inches(0.75)
    section.left_margin = Inches(0.8)
    section.right_margin = Inches(0.8)
    configure_styles(doc)
    add_page_number(section)

    title = doc.add_paragraph()
    title.style = "Title"
    title.alignment = WD_ALIGN_PARAGRAPH.CENTER
    title.add_run("ActiHome: guia tecnica de estudio")

    subtitle = doc.add_paragraph()
    subtitle.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run = subtitle.add_run(
        "Como se construye una aplicacion Java con Swing, Spring Boot, JPA y MySQL"
    )
    run.font.name = "Arial"
    run.font.size = Pt(12)
    run.font.color.rgb = RGBColor(90, 90, 90)

    doc.add_paragraph(
        "Este documento no explica la app como usuario. Explica la teoria y la forma de pensar que hay detras del codigo, para que puedas entenderlo, defenderlo y continuar desarrollandolo."
    )

    add_note(
        doc,
        "Idea central",
        "ActiHome esta hecha por capas: la interfaz recoge acciones, los servicios aplican reglas, los DAOs hablan con la base de datos y las entidades representan los datos.",
    )

    doc.add_heading("1. Mapa mental del proyecto", level=1)
    doc.add_paragraph(
        "La primera idea importante es que una aplicacion no se construye poniendo todo el codigo junto. Se divide en responsabilidades. En ActiHome cada paquete tiene un papel."
    )
    add_table(
        doc,
        ["Parte", "En el proyecto", "Responsabilidad"],
        [
            ["Arranque", "ActihomeApplication", "Inicia Spring Boot y abre la primera ventana Swing."],
            ["Interfaz", "ui", "Pantallas, botones, tablas y formularios."],
            ["Sesion", "SessionManager", "Recuerda que usuario ha iniciado sesion."],
            ["Servicios", "model/services", "Reglas de negocio: login, reserva, reviews, alojamientos."],
            ["Entidades", "model/entities", "Clases Java que representan tablas de la base de datos."],
            ["DAOs", "UserDao, HousingDao...", "Consultas y operaciones contra MySQL mediante Spring Data JPA."],
            ["Base de datos", "schema.sql / data.sql", "Estructura de tablas y datos iniciales."],
            ["Tests", "src/test", "Pruebas que validan la logica del proyecto."],
        ],
    )

    doc.add_heading("2. Arquitectura por capas", level=1)
    doc.add_paragraph(
        "La arquitectura por capas evita mezclar codigo de pantalla, reglas de negocio y base de datos. Esto hace que el proyecto sea mas facil de entender, probar y modificar."
    )
    add_code(
        doc,
        """
Usuario pulsa un boton en Swing
        |
        v
LoginFrame / ReserveHousingFrame / UploadHousingFrame
        |
        v
UserService / ReservationService / HousingService
        |
        v
UserDao / ReservationDao / HousingDao
        |
        v
MySQL
""",
    )
    doc.add_paragraph(
        "Ejemplo: una ventana no deberia comprobar si una contrasena es correcta ni guardar usuarios directamente. La ventana recoge datos y llama al servicio. El servicio decide y usa el DAO."
    )
    add_code(
        doc,
        """
sessionManager.login(userService.login(username, password));
""",
    )

    doc.add_heading("3. Spring Boot en una app de escritorio", level=1)
    doc.add_paragraph(
        "Normalmente Spring Boot se asocia con aplicaciones web, pero aqui se usa como motor interno para gestionar objetos, servicios, repositorios, transacciones y configuracion. La interfaz sigue siendo Swing."
    )
    add_code(
        doc,
        """
SpringApplication app = new SpringApplication(ActihomeApplication.class);
app.setWebApplicationType(WebApplicationType.NONE);
ConfigurableApplicationContext context = SpringApplication.run(ActihomeApplication.class, args);
""",
    )
    doc.add_paragraph(
        "La clave es WebApplicationType.NONE: se le dice a Spring que no levante un servidor web. Despues se obtiene una ventana desde el contenedor de Spring."
    )
    add_code(
        doc,
        """
EventQueue.invokeLater(() -> {
    LoginFrame loginFrame = context.getBean(LoginFrame.class);
    loginFrame.setVisible(true);
});
""",
    )

    doc.add_heading("4. Inyeccion de dependencias", level=1)
    doc.add_paragraph(
        "La inyeccion de dependencias significa que tus clases no crean manualmente todo lo que necesitan. Spring crea los objetos y se los entrega. Esto se ve con @Autowired o con parametros en el constructor."
    )
    add_code(
        doc,
        """
@Autowired
private UserDao userDao;
""",
    )
    doc.add_paragraph(
        "En vez de construir un UserDao con new, Spring proporciona una implementacion automaticamente. Esto permite cambiar piezas, testear mejor y mantener el codigo mas limpio."
    )
    add_table(
        doc,
        ["Anotacion", "Significado"],
        [
            ["@SpringBootApplication", "Marca la clase principal de arranque."],
            ["@Component", "Spring puede crear esta clase como objeto gestionado."],
            ["@Service", "Clase de logica de negocio gestionada por Spring."],
            ["@Autowired", "Spring inyecta una dependencia."],
            ["@Transactional", "Ejecuta operaciones dentro de una transaccion."],
            ["@Entity", "La clase representa una tabla de base de datos."],
        ],
    )

    doc.add_heading("5. Entidades: convertir el dominio en clases", level=1)
    doc.add_paragraph(
        "Una entidad es una clase que representa un concepto importante de la aplicacion y normalmente una tabla de la base de datos. En ActiHome las entidades principales son User, Housing, Reservation y Review."
    )
    add_code(
        doc,
        """
@Entity
@Table(name = "HOUSINGS")
public class Housing {
    private Long id;
    private Long housingCode;
    private String type;
    private int numberOfRooms;
    private BigDecimal pricePerNight;
    private User owner;
}
""",
    )
    doc.add_paragraph(
        "La clase Housing no es solo una bolsa de datos: es el modelo Java de un alojamiento. JPA se encarga de traducir sus campos a columnas."
    )
    add_table(
        doc,
        ["Campo Java", "Tipo", "Idea"],
        [
            ["id", "Long", "Identificador interno generado por la base de datos."],
            ["housingCode", "Long", "Codigo visible o de negocio del alojamiento."],
            ["pricePerNight", "BigDecimal", "Precio por noche, usando tipo exacto para dinero."],
            ["available", "boolean", "Indica si se puede reservar."],
            ["owner", "User", "Relacion con el usuario propietario."],
        ],
    )

    doc.add_heading("6. Relaciones entre entidades", level=1)
    doc.add_paragraph(
        "Las relaciones permiten expresar que un objeto depende de otro. Por ejemplo, un alojamiento tiene un propietario, una reserva tiene un cliente y un alojamiento, y una review tiene un autor y un alojamiento."
    )
    add_code(
        doc,
        """
@ManyToOne(optional = false, fetch = FetchType.EAGER)
@JoinColumn(name = "ownerId")
public User getOwner() {
    return owner;
}
""",
    )
    doc.add_paragraph(
        "ManyToOne significa muchos a uno. Muchos alojamientos pueden pertenecer a un mismo usuario. JoinColumn indica la columna de la tabla que guarda esa relacion."
    )
    add_table(
        doc,
        ["Relacion", "Tipo", "Explicacion"],
        [
            ["Housing -> User", "ManyToOne", "Muchos alojamientos pueden tener el mismo propietario."],
            ["Reservation -> User", "ManyToOne", "Muchas reservas pueden ser del mismo cliente."],
            ["Reservation -> Housing", "ManyToOne", "Muchas reservas historicas pueden referirse a un alojamiento."],
            ["Review -> User", "ManyToOne", "Muchas reviews pueden ser escritas por usuarios."],
            ["Review -> Housing", "ManyToOne", "Un alojamiento puede tener muchas reviews."],
        ],
    )

    doc.add_heading("7. DAOs y Spring Data JPA", level=1)
    doc.add_paragraph(
        "Un DAO o repository es la pieza que permite leer y guardar datos sin escribir todo el SQL manualmente. En tu proyecto extienden PagingAndSortingRepository."
    )
    add_code(
        doc,
        """
public interface UserDao extends PagingAndSortingRepository<User, Long> {
    boolean existsByUsername(String username);
    Optional<User> findByUsername(String username);
}
""",
    )
    doc.add_paragraph(
        "Spring interpreta el nombre de los metodos. findByUsername equivale conceptualmente a buscar en USERS por la columna username. existsByUsername devuelve true o false."
    )
    add_note(
        doc,
        "Optional",
        "Optional<User> expresa que puede haber usuario o puede no haberlo. Obliga a comprobar el caso vacio antes de usar el objeto.",
    )

    doc.add_heading("8. Servicios: donde vive la logica real", level=1)
    doc.add_paragraph(
        "Los servicios son la parte mas importante para entender como se hace la app. Aqui se aplican las reglas: quien puede reservar, cuando se puede hacer check-in, como se calcula una puntuacion, etc."
    )
    add_code(
        doc,
        """
public void signUp(User user) throws DuplicateInstanceException {
    if (userDao.existsByUsername(user.getUsername())) {
        throw new DuplicateInstanceException("project.entities.user", user);
    }

    user.setPassword(passwordEncoder.encode(user.getPassword()));
    userDao.save(user);
}
""",
    )
    doc.add_paragraph(
        "Este metodo registra un usuario. La teoria aplicada es sencilla: validar, transformar si hace falta y guardar. Si algo incumple una regla, se lanza una excepcion."
    )
    add_table(
        doc,
        ["Servicio", "Responsabilidad principal"],
        [
            ["UserService", "Registro, login, actualizar perfil y cambiar contrasena."],
            ["HousingService", "Crear, listar, filtrar, editar e intercambiar alojamientos."],
            ["ReservationService", "Reservar alojamiento, ver reservas y hacer check-in."],
            ["ReviewService", "Publicar, listar y actualizar reviews."],
            ["PermissionChecker", "Comprobar que usuarios existen y recuperar usuarios validos."],
        ],
    )

    doc.add_heading("9. Ejemplo completo: login", level=1)
    doc.add_paragraph(
        "El login es un buen ejemplo para entender el flujo completo desde la pantalla hasta la base de datos."
    )
    add_code(
        doc,
        """
private void login() {
    String username = usernameField.getText();
    String password = new String(passwordField.getPassword());

    sessionManager.login(userService.login(username, password));
}
""",
    )
    doc.add_paragraph(
        "La ventana recoge el texto del formulario. Despues llama a userService.login. El servicio busca el usuario y valida la contrasena."
    )
    add_code(
        doc,
        """
Optional<User> user = userDao.findByUsername(username);

if (!user.isPresent()) {
    throw new IncorrectLoginException(username, password);
}

if (!passwordEncoder.matches(password, user.get().getPassword())) {
    throw new IncorrectLoginException(username, password);
}

return user.get();
""",
    )
    doc.add_paragraph(
        "El password no se compara directamente con == ni equals. Se usa BCrypt porque la contrasena guardada esta cifrada. matches compara la contrasena introducida con el hash almacenado."
    )

    doc.add_heading("10. Ejemplo completo: reservar alojamiento", level=1)
    doc.add_paragraph(
        "La reserva muestra varias reglas de negocio encadenadas. Este es el tipo de metodo que conviene saber explicar en una entrevista."
    )
    add_code(
        doc,
        """
if (customer.getRole() != RoleType.CUSTOMER) {
    throw new NotAuthorizedUserException();
}

if (!housing.get().isAvailable()) {
    throw new AlreadyReservedException();
}

if (checkInDate.isBefore(reservationDate)) {
    throw new MustBeTodayOrAfterException();
}

if (checkOutDate.isBefore(checkInDate.plusDays(1))) {
    throw new CheckOutMustBeOneDayAfterException();
}
""",
    )
    doc.add_paragraph(
        "Fijate en el orden: primero se comprueba el rol, despues que el alojamiento exista y este disponible, despues las fechas y la tarjeta. Solo al final se crea la reserva."
    )
    add_code(
        doc,
        """
long nights = ChronoUnit.DAYS.between(
    checkInDate.toLocalDate(),
    checkOutDate.toLocalDate()
);

BigDecimal totalPrice = housing.get().getPricePerNight()
    .multiply(BigDecimal.valueOf(nights));

Reservation reservation = new Reservation(
    reservationCode,
    checkInDate,
    checkOutDate,
    "Tarjeta de credito",
    reservationDate,
    totalPrice,
    false,
    customer,
    housing.get()
);

housing.get().setAvailable(false);
return reservationDao.save(reservation);
""",
    )
    add_note(
        doc,
        "Transaccion",
        "Crear la reserva y marcar el alojamiento como no disponible deberia ocurrir como una unica operacion. Para eso sirve @Transactional.",
    )

    doc.add_heading("11. Excepciones propias", level=1)
    doc.add_paragraph(
        "Las excepciones propias convierten errores del negocio en clases con nombre. Esto hace que el codigo se lea mejor y que la interfaz pueda mostrar mensajes adecuados."
    )
    add_table(
        doc,
        ["Excepcion", "Cuando aparece"],
        [
            ["DuplicateInstanceException", "Se intenta registrar algo que ya existe."],
            ["IncorrectLoginException", "Usuario o contrasena incorrectos."],
            ["NotAuthorizedUserException", "El rol del usuario no permite esa accion."],
            ["AlreadyReservedException", "El alojamiento ya no esta disponible."],
            ["ScoreOutOfBoundsException", "Una puntuacion de review esta fuera de 0 a 5."],
            ["CannotCheckInException", "Se intenta hacer check-in antes de la fecha."],
        ],
    )
    doc.add_paragraph(
        "Una excepcion propia no significa que la aplicacion se haya roto. Muchas veces significa que una regla se ha cumplido correctamente: se ha bloqueado una accion invalida."
    )

    doc.add_heading("12. Swing: como se construyen las pantallas", level=1)
    doc.add_paragraph(
        "Swing trabaja con ventanas y componentes: JFrame, JPanel, JButton, JLabel, JTable, JTextField, etc. En tu proyecto cada pantalla suele extender JFrame."
    )
    add_code(
        doc,
        """
public class LoginFrame extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
}
""",
    )
    doc.add_paragraph(
        "La pantalla se monta creando componentes y asignando eventos. Un evento es una accion del usuario, por ejemplo pulsar un boton."
    )
    add_code(
        doc,
        """
JButton loginButton = new JButton("Iniciar sesion");
loginButton.addActionListener(e -> login());
""",
    )
    doc.add_paragraph(
        "La teoria aqui es programacion orientada a eventos: el programa no avanza de arriba abajo como un script, sino que espera acciones del usuario."
    )

    doc.add_heading("13. Sesion de usuario", level=1)
    doc.add_paragraph(
        "SessionManager guarda el usuario que ha iniciado sesion. Es una clase sencilla, pero importante porque permite que las ventanas sepan quien esta usando la app."
    )
    add_code(
        doc,
        """
private User loggedInUser;

public void login(User user) {
    loggedInUser = user;
}

public boolean isUserLoggedIn() {
    return loggedInUser != null;
}
""",
    )
    doc.add_paragraph(
        "Este patron es comun: guardar el estado de sesion en un componente compartido. En una app web esto se haria de otra forma, pero en Swing tiene sentido."
    )

    doc.add_heading("14. Base de datos y SQL", level=1)
    doc.add_paragraph(
        "schema.sql define la estructura de la base de datos. data.sql mete datos iniciales. Aunque JPA trabaja con objetos, por debajo todo termina en tablas."
    )
    add_code(
        doc,
        """
CREATE TABLE HOUSINGS (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    housingCode BIGINT NOT NULL,
    type VARCHAR(30) NOT NULL,
    numberOfRooms INTEGER NOT NULL,
    pricePerNight DECIMAL(10, 2) NOT NULL,
    ownerId BIGINT NOT NULL,
    CONSTRAINT OwnerIdFK FOREIGN KEY(ownerId)
    REFERENCES Users(id)
);
""",
    )
    doc.add_paragraph(
        "La columna ownerId es una clave externa. Eso significa que un alojamiento no guarda todo el usuario dentro, sino el id del usuario propietario."
    )

    doc.add_heading("15. Tests: aprender el codigo por casos", level=1)
    doc.add_paragraph(
        "Los tests explican que espera el proyecto de cada servicio. Son casi una documentacion ejecutable."
    )
    add_code(
        doc,
        """
assertThrows(DuplicateInstanceException.class,
    () -> userService.signUp(user2));
""",
    )
    doc.add_paragraph(
        "Este test dice: si intento registrar dos usuarios con el mismo username, el sistema debe lanzar DuplicateInstanceException. No solo prueba codigo: documenta una regla."
    )
    add_table(
        doc,
        ["Tipo de test", "Que comprueba"],
        [
            ["Caso correcto", "La accion funciona y devuelve lo esperado."],
            ["Caso de error", "La accion se bloquea con la excepcion correcta."],
            ["Orden", "Listados de reservas o reviews aparecen en el orden esperado."],
            ["Calculos", "Precio total o puntuacion media se calculan bien."],
        ],
    )
    add_note(
        doc,
        "Detalle observado",
        "Los reportes guardados indican que algunos tests de HousingService fallaban por orden/datos de prueba mezclados. Es un buen punto de mejora para portfolio.",
    )

    doc.add_heading("16. Como se construiria desde cero", level=1)
    doc.add_paragraph(
        "Si tuvieras que rehacer ActiHome desde cero, el orden recomendable seria este:"
    )
    steps = [
        "Definir requisitos: roles, acciones y reglas principales.",
        "Disenar el modelo: User, Housing, Reservation y Review.",
        "Disenar la base de datos y sus relaciones.",
        "Crear entidades JPA con @Entity, @Id, @ManyToOne y @JoinColumn.",
        "Crear DAOs para operaciones basicas y consultas necesarias.",
        "Crear servicios con interfaces e implementaciones.",
        "Crear excepciones propias para reglas incumplidas.",
        "Crear tests de servicios antes de pulir la interfaz.",
        "Crear ventanas Swing que llamen a los servicios.",
        "Probar el flujo completo y corregir errores.",
    ]
    for step in steps:
        doc.add_paragraph(step, style="List Number")

    doc.add_heading("17. Conceptos que debes dominar", level=1)
    add_table(
        doc,
        ["Concepto", "Para que te sirve en ActiHome"],
        [
            ["POO", "Crear clases que representan conceptos reales."],
            ["Encapsulacion", "Proteger datos mediante atributos privados y getters/setters."],
            ["Interfaces", "Definir contratos como UserService sin depender de una implementacion concreta."],
            ["Spring Boot", "Arranque, configuracion e inyeccion de dependencias."],
            ["JPA", "Mapear objetos Java a tablas SQL."],
            ["Repositories", "Consultar la base de datos con metodos Java."],
            ["Transacciones", "Evitar operaciones guardadas a medias."],
            ["Excepciones", "Representar errores de reglas de negocio."],
            ["Swing", "Crear interfaz de escritorio basada en eventos."],
            ["JUnit", "Probar que las reglas funcionan."],
        ],
    )

    doc.add_heading("18. Mejoras para portfolio", level=1)
    doc.add_paragraph(
        "Para convertir el proyecto en algo mas presentable, no hace falta reescribirlo todo. Puedes mejorar por fases."
    )
    for item in [
        "Crear un README profesional con capturas, tecnologias y funcionalidades.",
        "Arreglar los tests que fallan y explicar el aprendizaje.",
        "Corregir textos con caracteres raros por problemas de codificacion.",
        "Anadir validaciones mas completas en formularios.",
        "Separar mejor logica de interfaz en algunas ventanas.",
        "Crear datos de prueba limpios.",
        "Anadir capturas o un pequeno video de uso.",
        "Opcional: migrar la interfaz a JavaFX o crear una API REST con frontend web.",
    ]:
        doc.add_paragraph(item, style="List Bullet")

    doc.add_heading("19. Frase para defender el proyecto", level=1)
    doc.add_paragraph(
        "ActiHome es una aplicacion de escritorio desarrollada en Java con Swing y Spring Boot. Usa una arquitectura por capas, persistencia con JPA sobre MySQL, servicios transaccionales para la logica de negocio, excepciones propias para validar reglas del dominio y tests con JUnit para comprobar los casos principales."
    )

    doc.add_heading("20. Ruta de estudio recomendada", level=1)
    for item in [
        "Leer primero las entidades y dibujar las tablas.",
        "Leer los DAOs y traducir cada metodo a una consulta SQL mental.",
        "Leer UserServiceImpl completo hasta poder explicarlo sin mirar.",
        "Leer ReservationServiceImpl porque concentra muchas reglas reales.",
        "Leer una ventana Swing y seguir el flujo boton -> servicio -> DAO.",
        "Leer los tests como si fueran ejemplos de uso.",
        "Modificar una funcionalidad pequena y crear su test.",
    ]:
        doc.add_paragraph(item, style="List Number")

    doc.add_paragraph()
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = p.add_run("Fin de la guia")
    r.bold = True
    r.font.name = "Arial"
    r.font.size = Pt(11)
    r.font.color.rgb = RGBColor(90, 90, 90)

    doc.save(OUTPUT)


if __name__ == "__main__":
    build()
